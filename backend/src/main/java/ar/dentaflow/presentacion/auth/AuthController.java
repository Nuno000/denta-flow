package ar.dentaflow.presentacion.auth;

import ar.dentaflow.aplicacion.usuarios.ResultadoIngreso;
import ar.dentaflow.aplicacion.usuarios.ServicioDeIngreso;
import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.aplicacion.usuarios.UsuarioDetalle;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.PersonalInterno;
import ar.dentaflow.presentacion.usuarios.UsuarioRespuesta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ingreso del personal y consulta del usuario autenticado (spec autenticacion-personal, design D10). */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Ingreso del personal con email y contraseña")
public class AuthController {

    private final ServicioDeIngreso ingreso;
    private final ServicioDeUsuarios usuarios;

    public AuthController(ServicioDeIngreso ingreso, ServicioDeUsuarios usuarios) {
        this.ingreso = ingreso;
        this.usuarios = usuarios;
    }

    /** Pública: es la única forma de obtener un token. */
    @PostMapping("/login")
    @Operation(summary = "Ingresar", description = "Devuelve un token JWT que vence a las 8 horas")
    @ApiResponse(responseCode = "200", description = "Ingreso correcto")
    @ApiResponse(responseCode = "400", description = "Datos de ingreso inválidos")
    @ApiResponse(responseCode = "401", description = "Email o contraseña incorrectos, o usuario desactivado")
    @ApiResponse(responseCode = "429", description = "Demasiados intentos fallidos; ver Retry-After")
    public LoginRespuesta ingresar(@Valid @RequestBody LoginSolicitud solicitud) {
        ResultadoIngreso resultado = ingreso.ingresar(solicitud.email(), solicitud.contrasena());
        UsuarioDetalle detalle = usuarios.obtener(resultado.usuario().getId());
        return new LoginRespuesta(resultado.token().token(), resultado.token().venceEn(), UsuarioRespuesta.de(detalle));
    }

    @PersonalInterno
    @GetMapping("/yo")
    @Operation(summary = "Usuario autenticado", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Datos del usuario que hizo el pedido")
    @ApiResponse(responseCode = "401", description = "Sin token válido")
    public UsuarioRespuesta yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioRespuesta.de(usuarios.obtener(usuario.id()));
    }
}

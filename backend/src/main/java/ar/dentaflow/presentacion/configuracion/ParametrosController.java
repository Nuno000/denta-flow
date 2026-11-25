package ar.dentaflow.presentacion.configuracion;

import ar.dentaflow.aplicacion.configuracion.ServicioDeParametros;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta y edición de los parámetros del consultorio por el administrador (spec
 * parametros-consultorio, design D7). Catálogo cerrado: no hay alta ni baja, solo se cambia el valor.
 */
@RestController
@RequestMapping("/api/parametros")
@Tag(name = "Parámetros", description = "Parámetros de configuración del consultorio, solo para el administrador")
@SecurityRequirement(name = "bearerAuth")
public class ParametrosController {

    private final ServicioDeParametros servicio;

    public ParametrosController(ServicioDeParametros servicio) {
        this.servicio = servicio;
    }

    @SoloAdministrador
    @GetMapping
    @Operation(summary = "Listar los parámetros", description = "Los tres parámetros, en el orden del catálogo")
    @ApiResponse(responseCode = "200", description = "Parámetros con su valor y marca provisoria")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    public List<ParametroRespuesta> listar() {
        return servicio.listar().stream().map(ParametroRespuesta::de).toList();
    }

    @SoloAdministrador
    @GetMapping("/{clave}")
    @Operation(summary = "Consultar un parámetro")
    @ApiResponse(responseCode = "200", description = "El parámetro")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "La clave no está en el catálogo")
    public ParametroRespuesta obtener(@PathVariable String clave) {
        return ParametroRespuesta.de(servicio.obtener(clave));
    }

    @SoloAdministrador
    @PutMapping("/{clave}")
    @Operation(summary = "Cambiar el valor de un parámetro",
            description = "Guardar confirma el valor (deja de ser provisorio), aunque sea el mismo")
    @ApiResponse(responseCode = "200", description = "Parámetro actualizado")
    @ApiResponse(responseCode = "400", description = "Valor faltante, fuera de rango o demasiado largo")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "La clave no está en el catálogo")
    public ParametroRespuesta actualizar(@AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable String clave, @Valid @RequestBody ActualizarParametroSolicitud solicitud) {
        return ParametroRespuesta.de(servicio.actualizar(clave, solicitud.valor(), actor.id()));
    }
}

package ar.dentaflow.presentacion.usuarios;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de usuarios por el administrador (US-003, spec gestion-usuarios, design D10). Sin borrado: la
 * única baja es la desactivación. Todas las operaciones son solo para el rol administrador.
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles por el administrador")
@SecurityRequirement(name = "bearerAuth")
public class UsuariosController {

    private final ServicioDeUsuarios servicio;

    public UsuariosController(ServicioDeUsuarios servicio) {
        this.servicio = servicio;
    }

    @SoloAdministrador
    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Activos e inactivos, por id, sin datos de la credencial")
    public List<UsuarioRespuesta> listar() {
        return servicio.listar().stream().map(UsuarioRespuesta::de).toList();
    }

    @SoloAdministrador
    @PostMapping
    @Operation(summary = "Crear un usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creado y activo")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, rol inexistente o vínculo con profesional incorrecto")
    @ApiResponse(responseCode = "409", description = "Email en uso o profesional que ya tiene usuario")
    public ResponseEntity<UsuarioRespuesta> crear(@Valid @RequestBody CrearUsuarioSolicitud solicitud) {
        UsuarioRespuesta creado = UsuarioRespuesta.de(servicio.crear(solicitud.email(), solicitud.contrasena(),
                Rol.desdeCodigo(solicitud.rol()), solicitud.profesionalId()));
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    @SoloAdministrador
    @PutMapping("/{id}/rol")
    @Operation(summary = "Cambiar el rol (y el profesional vinculado) de otro usuario")
    @ApiResponse(responseCode = "409", description = "Sobre uno mismo, o profesional que ya tiene usuario")
    public UsuarioRespuesta cambiarRol(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id,
            @Valid @RequestBody CambiarRolSolicitud solicitud) {
        return UsuarioRespuesta.de(servicio.cambiarRol(actor.id(), id, Rol.desdeCodigo(solicitud.rol()),
                solicitud.profesionalId()));
    }

    @SoloAdministrador
    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar a otro usuario",
            description = "Deja de poder ingresar y operar desde el request siguiente")
    @ApiResponse(responseCode = "409", description = "Sobre uno mismo")
    public UsuarioRespuesta desactivar(@AuthenticationPrincipal UsuarioAutenticado actor, @PathVariable Long id) {
        return UsuarioRespuesta.de(servicio.desactivar(actor.id(), id));
    }

    @SoloAdministrador
    @PostMapping("/{id}/activar")
    @Operation(summary = "Reactivar un usuario")
    public UsuarioRespuesta activar(@PathVariable Long id) {
        return UsuarioRespuesta.de(servicio.activar(id));
    }

    @SoloAdministrador
    @PutMapping("/{id}/contrasena")
    @Operation(summary = "Blanquear la contraseña de otro usuario")
    @ApiResponse(responseCode = "204", description = "Contraseña cambiada; la anterior deja de funcionar")
    @ApiResponse(responseCode = "409", description = "Sobre uno mismo")
    public ResponseEntity<Void> blanquearContrasena(@AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable Long id, @Valid @RequestBody BlanqueoSolicitud solicitud) {
        servicio.blanquearContrasena(actor.id(), id, solicitud.contrasena());
        return ResponseEntity.noContent().build();
    }

    @SoloAdministrador
    @GetMapping("/profesionales-disponibles")
    @Operation(summary = "Profesionales activos sin usuario, para vincular uno con rol profesional")
    public List<ProfesionalDisponible> profesionalesDisponibles() {
        return servicio.listarProfesionalesDisponibles().stream().map(ProfesionalDisponible::de).toList();
    }
}

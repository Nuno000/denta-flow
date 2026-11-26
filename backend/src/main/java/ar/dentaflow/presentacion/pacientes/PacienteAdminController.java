package ar.dentaflow.presentacion.pacientes;

import ar.dentaflow.aplicacion.pacientes.ServicioDeAnonimizacion;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Baja de pacientes por anonimización (C-15, Ley 25.326, PA-12): solo el administrador. */
@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Pacientes", description = "Datos personales de pacientes (Ley 25.326)")
@SecurityRequirement(name = "bearerAuth")
public class PacienteAdminController {

    private final ServicioDeAnonimizacion anonimizacion;

    public PacienteAdminController(ServicioDeAnonimizacion anonimizacion) {
        this.anonimizacion = anonimizacion;
    }

    @SoloAdministrador
    @PostMapping("/{id}/anonimizar")
    @Operation(summary = "Dar de baja a un paciente anonimizando sus datos",
            description = "Borra el DNI y reemplaza nombre, apellido, teléfono y email. Conserva sus turnos "
                    + "(retención de 5 años). Repetirlo no cambia nada.")
    @ApiResponse(responseCode = "200", description = "Paciente anonimizado")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El paciente no existe")
    @ApiResponse(responseCode = "409", description = "Tiene turnos pendientes o confirmados por venir")
    public PacienteAnonimizadoRespuesta anonimizar(@AuthenticationPrincipal UsuarioAutenticado actor,
            @PathVariable long id) {
        return PacienteAnonimizadoRespuesta.de(anonimizacion.anonimizar(id, actor.id()));
    }
}

package ar.dentaflow.presentacion.auditoria;

import ar.dentaflow.aplicacion.auditoria.ConsultaDeHistorialDeTurno;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta de auditoría por turno (C-13, US-025; spec auditoria-turnos; design D2). Solo lectura y restringido
 * estrictamente al rol {@code administrador}: no existen operaciones de modificación ni borrado.
 */
@RestController
@RequestMapping("/api/turnos")
@Tag(name = "Auditoría", description = "Registro de cambios de turnos (solo lectura, administrador)")
@SecurityRequirement(name = "bearerAuth")
public class HistorialTurnoController {

    private final ConsultaDeHistorialDeTurno consulta;

    public HistorialTurnoController(ConsultaDeHistorialDeTurno consulta) {
        this.consulta = consulta;
    }

    @SoloAdministrador
    @GetMapping("/{id}/historial")
    @Operation(summary = "Consultar el registro de cambios de un turno",
            description = "Devuelve los cambios auditados del turno en orden cronológico ascendente, con el actor, "
                    + "la fecha y hora en UTC, la acción y los valores anterior y nuevo. Exclusivo del administrador.")
    @ApiResponse(responseCode = "200", description = "Historial cronológico del turno")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "Solo el administrador puede consultar la auditoría")
    @ApiResponse(responseCode = "404", description = "El turno no existe")
    public List<CambioDeTurnoRespuesta> historial(@PathVariable long id) {
        return consulta.consultar(id).stream()
                .map(CambioDeTurnoRespuesta::de)
                .toList();
    }
}

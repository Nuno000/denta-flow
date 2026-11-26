package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.aplicacion.turnos.RegistroDeAsistencia;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.PersonalInterno;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro de asistencia de un turno (C-13, US-024; spec asistencia-turnos; design D1, D3). Los tres roles internos
 * pueden usarlo; el profesional solo sobre sus propios turnos (verificado en {@link RegistroDeAsistencia}).
 */
@RestController
@RequestMapping("/api/turnos")
@Tag(name = "Turnos", description = "Acciones operativas sobre turnos del consultorio")
@SecurityRequirement(name = "bearerAuth")
public class AsistenciaController {

    private final RegistroDeAsistencia registro;

    public AsistenciaController(RegistroDeAsistencia registro) {
        this.registro = registro;
    }

    @PersonalInterno
    @PostMapping("/{id}/asistencia")
    @Operation(summary = "Registrar la asistencia de un turno",
            description = "Registra asistio, ausente o ausente_con_aviso y audita el cambio en la misma transacción. "
                    + "asistio y ausente requieren que el turno ya haya empezado; ausente_con_aviso se acepta en "
                    + "cualquier momento. El rol profesional solo puede registrar en sus propios turnos.")
    @ApiResponse(responseCode = "200", description = "Asistencia registrada (idempotente si repite el estado)")
    @ApiResponse(responseCode = "400", description = "Estado inválido o cuerpo mal formado")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "Profesional sobre turno ajeno o sin vínculo")
    @ApiResponse(responseCode = "404", description = "El turno no existe")
    @ApiResponse(responseCode = "409", description = "Turno cancelado o asistencia registrada antes del inicio")
    public AsistenciaRespuesta registrar(@PathVariable long id,
            @Valid @RequestBody SolicitudDeAsistencia solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Turno actualizado = registro.registrar(id, solicitud.estado(), usuario.rol(), usuario.id(),
                usuario.profesionalId());
        return AsistenciaRespuesta.de(actualizado);
    }
}

package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.turnos.Turno;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * Turno recién cargado (C-12, design D1). Sin token de acceso ni datos de contacto del paciente (Ley 25.326); los
 * instantes llevan el desplazamiento de {@code America/Argentina/Buenos_Aires}.
 */
public record TurnoCargadoRespuesta(
        @Schema(description = "Identificador del turno") Long id,
        @Schema(description = "Inicio con el desplazamiento del consultorio", example = "2026-12-01T09:00:00-03:00")
        OffsetDateTime inicio,
        @Schema(description = "Fin con el desplazamiento del consultorio", example = "2026-12-01T09:30:00-03:00")
        OffsetDateTime fin,
        @Schema(description = "Estado inicial: pendiente", example = "pendiente") String estado,
        @Schema(description = "Verdadero solo si se cargó por la operación de sobreturno") boolean esSobreturno,
        Long pacienteId,
        Long prestacionId,
        Long profesionalId,
        Long sillonId) {

    static TurnoCargadoRespuesta de(Turno turno) {
        return new TurnoCargadoRespuesta(turno.getId(),
                turno.getInicio().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                turno.getFin().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                turno.getEstado().codigo(), turno.isEsSobreturno(), turno.getPacienteId(), turno.getPrestacionId(),
                turno.getProfesionalId(), turno.getSillonId());
    }
}

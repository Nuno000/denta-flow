package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.dominio.turnos.Turno;
import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta mínima tras registrar asistencia: sin datos personales del paciente (Ley 25.326). */
public record AsistenciaRespuesta(
        @Schema(description = "Identificador del turno") Long id,
        @Schema(description = "Estado resultante del turno") String estado) {

    static AsistenciaRespuesta de(Turno turno) {
        return new AsistenciaRespuesta(turno.getId(), turno.getEstado().codigo());
    }
}

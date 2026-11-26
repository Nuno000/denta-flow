package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.aplicacion.disponibilidad.ResultadoBloqueo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Bloqueo guardado y aviso de los turnos que quedan dentro de su franja. */
public record ResultadoBloqueoRespuesta(
        BloqueoRespuesta bloqueo,
        @Schema(description = "Turnos activos dentro de la franja del bloqueo; no se modifican. "
                + "Hasta que exista la agenda de turnos la lista es vacía: se completa a partir de C-09")
        List<TurnoAfectadoRespuesta> turnosAfectados) {

    static ResultadoBloqueoRespuesta de(ResultadoBloqueo resultado) {
        return new ResultadoBloqueoRespuesta(BloqueoRespuesta.de(resultado.bloqueo()),
                resultado.turnosAfectados().stream().map(TurnoAfectadoRespuesta::de).toList());
    }
}

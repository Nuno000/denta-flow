package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.aplicacion.disponibilidad.TurnoAfectado;
import java.time.OffsetDateTime;

/** Turno activo dentro de la franja de un bloqueo; solo se informa (PA-11). */
public record TurnoAfectadoRespuesta(Long turnoId, OffsetDateTime inicio, OffsetDateTime fin, Long profesionalId,
        Long sillonId) {

    static TurnoAfectadoRespuesta de(TurnoAfectado turno) {
        return new TurnoAfectadoRespuesta(turno.turnoId(), BloqueoRespuesta.enZonaDelConsultorio(turno.inicio()),
                BloqueoRespuesta.enZonaDelConsultorio(turno.fin()), turno.profesionalId(), turno.sillonId());
    }
}

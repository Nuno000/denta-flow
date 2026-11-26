package ar.dentaflow.aplicacion.disponibilidad;

import java.time.Instant;

/** Turno activo que cae dentro de la franja de un bloqueo; solo se informa, no se modifica (PA-11). */
public record TurnoAfectado(Long turnoId, Instant inicio, Instant fin, Long profesionalId, Long sillonId) {
}

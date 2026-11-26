package ar.dentaflow.aplicacion.turnos;

import java.time.Instant;

/** Destino de una reprogramación: nuevo inicio, profesional y sillón (pueden coincidir con los actuales). */
public record SolicitudDeReprogramacion(Instant inicio, long profesionalId, long sillonId) {
}

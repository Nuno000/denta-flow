package ar.dentaflow.aplicacion.reservas;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import java.time.Instant;

/**
 * Resumen de la reserva recién confirmada para el paciente (C-16 design D2). No contiene tokens, sillón ni datos
 * personales.
 */
public record ReservaConfirmada(
        Instant inicio,
        Instant fin,
        String prestacion,
        String profesional,
        EstadoTurno estado) {
}

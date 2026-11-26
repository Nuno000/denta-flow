package ar.dentaflow.aplicacion.turnos;

import java.time.Instant;

public record TurnoCanceladoEvent(
        long turnoId,
        long prestacionId,
        long profesionalId,
        Instant inicio) {
}

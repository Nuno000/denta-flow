package ar.dentaflow.dominio.turnos;

import java.time.Instant;

/** Datos de un turno que está por crearse o moverse, que las {@link RestriccionDeAgenda} evalúan. */
public record TurnoPropuesto(Long profesionalId, Long sillonId, Long prestacionId, Instant inicio, Instant fin,
        boolean esSobreturno) {
}

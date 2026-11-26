package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import java.time.Instant;
import java.util.List;

/**
 * Criterio ya validado para leer la agenda: turnos cuyo {@code [inicio, fin)} se cruza con {@code [desde, hasta)}
 * (instantes UTC), con los estados indicados y, si no son nulos, del profesional y del sillón dados.
 */
public record CriterioDeAgenda(Instant desde, Instant hasta, Long profesionalId, Long sillonId,
        List<EstadoTurno> estados) {

    public CriterioDeAgenda {
        estados = List.copyOf(estados);
    }
}

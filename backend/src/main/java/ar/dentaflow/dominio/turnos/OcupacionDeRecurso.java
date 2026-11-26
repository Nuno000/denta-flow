package ar.dentaflow.dominio.turnos;

import java.time.Instant;

/**
 * Intervalo semiabierto {@code [inicio, fin)} en que un recurso (profesional o sillón) está
 * ocupado por un turno activo no sobreturno (C-10).
 */
public record OcupacionDeRecurso(Long recursoId, Instant inicio, Instant fin) {

    public boolean seSuperponeCon(Instant desde, Instant hasta) {
        return inicio.isBefore(hasta) && desde.isBefore(fin);
    }
}

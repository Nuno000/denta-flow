package ar.dentaflow.dominio.disponibilidad;

import java.time.Instant;
import java.util.List;

/** Hueco ofrecido {@code [inicio, fin)} con sus candidatos ordenados (PA-05, PA-06). */
public record Hueco(Instant inicio, Instant fin, List<CandidatoDeReserva> candidatos) {

    public Hueco {
        candidatos = List.copyOf(candidatos);
    }
}

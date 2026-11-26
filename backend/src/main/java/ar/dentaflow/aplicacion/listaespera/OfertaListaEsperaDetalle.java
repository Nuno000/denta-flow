package ar.dentaflow.aplicacion.listaespera;

import java.time.Instant;

public record OfertaListaEsperaDetalle(
        String estado,
        String prestacionNombre,
        String profesionalNombre,
        Instant inicioOfrecido,
        Instant ofrecidaHasta,
        Long turnoCreadoId) {
}

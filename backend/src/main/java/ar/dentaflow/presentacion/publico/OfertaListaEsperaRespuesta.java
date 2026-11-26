package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.listaespera.OfertaListaEsperaDetalle;
import java.time.Instant;

public record OfertaListaEsperaRespuesta(
        String estado,
        String prestacionNombre,
        String profesionalNombre,
        Instant inicioOfrecido,
        Instant ofrecidaHasta,
        Long turnoCreadoId) {

    public static OfertaListaEsperaRespuesta de(OfertaListaEsperaDetalle detalle) {
        return new OfertaListaEsperaRespuesta(
                detalle.estado(),
                detalle.prestacionNombre(),
                detalle.profesionalNombre(),
                detalle.inicioOfrecido(),
                detalle.ofrecidaHasta(),
                detalle.turnoCreadoId());
    }
}

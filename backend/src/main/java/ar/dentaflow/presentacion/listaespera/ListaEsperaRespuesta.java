package ar.dentaflow.presentacion.listaespera;

import ar.dentaflow.aplicacion.listaespera.ListaEsperaDetalle;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ListaEsperaRespuesta(
        Long id,
        long pacienteId,
        String pacienteNombre,
        String pacienteDni,
        long prestacionId,
        String prestacionNombre,
        Long profesionalId,
        String profesionalNombre,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalTime horaDesde,
        LocalTime horaHasta,
        String estado,
        Instant inicioOfrecido,
        Instant ofrecidaHasta,
        Instant creadoEn) {

    public static ListaEsperaRespuesta de(ListaEsperaDetalle detalle) {
        return new ListaEsperaRespuesta(
                detalle.id(),
                detalle.pacienteId(),
                detalle.pacienteNombre(),
                detalle.pacienteDni(),
                detalle.prestacionId(),
                detalle.prestacionNombre(),
                detalle.profesionalId(),
                detalle.profesionalNombre(),
                detalle.fechaDesde(),
                detalle.fechaHasta(),
                detalle.horaDesde(),
                detalle.horaHasta(),
                detalle.estado(),
                detalle.inicioOfrecido(),
                detalle.ofrecidaHasta(),
                detalle.creadoEn());
    }
}

package ar.dentaflow.aplicacion.listaespera;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public record ListaEsperaDetalle(
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
}

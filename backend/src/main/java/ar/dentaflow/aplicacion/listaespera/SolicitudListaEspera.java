package ar.dentaflow.aplicacion.listaespera;

import java.time.LocalDate;
import java.time.LocalTime;

public record SolicitudListaEspera(
        long pacienteId,
        long prestacionId,
        Long profesionalId,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        LocalTime horaDesde,
        LocalTime horaHasta) {
}

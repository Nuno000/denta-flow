package ar.dentaflow.presentacion.listaespera;

import ar.dentaflow.aplicacion.listaespera.SolicitudListaEspera;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record SolicitudCrearListaEspera(
        @NotNull(message = "El paciente es obligatorio") Long pacienteId,
        @NotNull(message = "La prestación es obligatoria") Long prestacionId,
        Long profesionalId,
        @NotNull(message = "La fecha desde es obligatoria") LocalDate fechaDesde,
        @NotNull(message = "La fecha hasta es obligatoria") LocalDate fechaHasta,
        LocalTime horaDesde,
        LocalTime horaHasta) {

    public SolicitudListaEspera aSolicitud() {
        return new SolicitudListaEspera(pacienteId, prestacionId, profesionalId, fechaDesde, fechaHasta,
                horaDesde, horaHasta);
    }
}

package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Franja de atención de una fecha del consultorio, {@code [inicio, fin)} en hora local. */
public record FranjaLocal(LocalDate fecha, LocalTime inicio, LocalTime fin) {

    public Instant inicioInstante() {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(fecha, inicio));
    }

    public Instant finInstante() {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(fecha, fin));
    }
}

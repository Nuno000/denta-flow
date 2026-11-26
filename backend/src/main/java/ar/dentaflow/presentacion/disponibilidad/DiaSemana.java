package ar.dentaflow.presentacion.disponibilidad;

import java.time.DayOfWeek;

/** Día de la semana en la API, en español; se traduce a {@link DayOfWeek} (ISO, lunes primero). */
public enum DiaSemana {
    LUNES(DayOfWeek.MONDAY),
    MARTES(DayOfWeek.TUESDAY),
    MIERCOLES(DayOfWeek.WEDNESDAY),
    JUEVES(DayOfWeek.THURSDAY),
    VIERNES(DayOfWeek.FRIDAY),
    SABADO(DayOfWeek.SATURDAY),
    DOMINGO(DayOfWeek.SUNDAY);

    private final DayOfWeek dia;

    DiaSemana(DayOfWeek dia) {
        this.dia = dia;
    }

    public DayOfWeek aDayOfWeek() {
        return dia;
    }

    public static DiaSemana de(DayOfWeek dia) {
        return values()[dia.getValue() - 1];
    }
}

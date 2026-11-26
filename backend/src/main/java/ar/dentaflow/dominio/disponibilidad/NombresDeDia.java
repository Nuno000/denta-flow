package ar.dentaflow.dominio.disponibilidad;

import java.time.DayOfWeek;

/** Nombre en español del día de la semana, para mensajes de dominio. */
final class NombresDeDia {

    private static final String[] NOMBRES = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private NombresDeDia() {
    }

    static String de(DayOfWeek dia) {
        return NOMBRES[dia.getValue() - 1];
    }
}

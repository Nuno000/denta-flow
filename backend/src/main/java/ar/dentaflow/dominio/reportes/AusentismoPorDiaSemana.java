package ar.dentaflow.dominio.reportes;

import java.time.DayOfWeek;

/** Indicadores de ausentismo agrupados por día de la semana en hora del consultorio. */
public record AusentismoPorDiaSemana(
        DayOfWeek diaSemana,
        String etiqueta,
        ResumenAusentismo resumen) {
}

package ar.dentaflow.dominio.reportes;

import java.time.LocalDate;
import java.util.List;

/** Resultado consolidado de métricas de ausentismo para un rango de fechas (C-23, US-031). */
public record ReporteDeAusentismo(
        LocalDate desde,
        LocalDate hasta,
        Long profesionalId,
        int minimoAusencias,
        ResumenAusentismo general,
        List<AusentismoPorProfesional> porProfesional,
        List<AusentismoPorFranja> porFranja,
        List<AusentismoPorDiaSemana> porDiaSemana,
        List<AusentismoPorPaciente> pacientesReiterados) {
}

package ar.dentaflow.dominio.reportes;

/** Indicadores de ausentismo agrupados para un profesional. */
public record AusentismoPorProfesional(
        long profesionalId,
        String nombre,
        String apellido,
        ResumenAusentismo resumen) {
}

package ar.dentaflow.dominio.reportes;

/** Indicadores de ausentismo agrupados para una franja horaria del consultorio. */
public record AusentismoPorFranja(
        FranjaHoraria franja,
        ResumenAusentismo resumen) {
}

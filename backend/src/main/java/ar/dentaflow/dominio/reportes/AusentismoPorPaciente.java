package ar.dentaflow.dominio.reportes;

/** Indicadores de ausentismo de un paciente sin exponer datos de contacto ni DNI (Ley 25.326). */
public record AusentismoPorPaciente(
        long pacienteId,
        String nombre,
        String apellido,
        ResumenAusentismo resumen) {
}

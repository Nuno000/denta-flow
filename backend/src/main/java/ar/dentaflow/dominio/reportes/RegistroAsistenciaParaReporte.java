package ar.dentaflow.dominio.reportes;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import java.time.Instant;

/**
 * Proyección mínima de un turno evaluado para calcular indicadores de ausentismo (C-23, design D2 y D5).
 * Nunca incluye DNI, teléfono, email ni token del paciente (Ley 25.326, RN-PR-03).
 */
public record RegistroAsistenciaParaReporte(
        Instant inicio,
        EstadoTurno estado,
        long profesionalId,
        String profesionalNombre,
        String profesionalApellido,
        long pacienteId,
        String pacienteNombre,
        String pacienteApellido) {
}

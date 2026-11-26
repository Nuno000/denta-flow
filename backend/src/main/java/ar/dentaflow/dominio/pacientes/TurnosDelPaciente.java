package ar.dentaflow.dominio.pacientes;

import java.time.Instant;

/**
 * Consulta de solo lectura sobre los turnos de un paciente que necesita la baja por anonimización (C-15). Es un
 * puerto aparte para que quien da de baja no dependa del repositorio de turnos, cuya escritura es exclusiva de
 * {@code ServicioDeTurnos} (RN-GL-02).
 */
public interface TurnosDelPaciente {

    /** Hay turnos {@code pendiente} o {@code confirmado} del paciente que empiezan después de {@code ahora}. */
    boolean hayActivosFuturos(long pacienteId, Instant ahora);
}

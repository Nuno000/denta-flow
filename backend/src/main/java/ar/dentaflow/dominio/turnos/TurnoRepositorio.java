package ar.dentaflow.dominio.turnos;

import java.time.Instant;
import java.util.Optional;

/**
 * Puerto de persistencia de turnos. {@link #guardar} solo lo invoca {@code ServicioDeTurnos}, la única vía de
 * escritura de turnos (RN-GL-02). Sin borrado: un turno se cancela, no se elimina.
 */
public interface TurnoRepositorio {

    Turno guardar(Turno turno);

    Optional<Turno> buscarPorId(Long id);

    /** Igual que {@link #buscarPorId} pero bloquea la fila del turno hasta el fin de la transacción. */
    Optional<Turno> buscarPorIdParaActualizar(Long id);

    /** Busca por token de acceso; un texto con formato inválido devuelve vacío sin consultar la base. */
    Optional<Turno> buscarPorToken(String token);

    /**
     * Hay otro turno activo (no cancelado, no sobreturno) del profesional que se superpone con
     * {@code [inicio, fin)}. {@code excluirTurnoId} permite ignorar al propio turno al reprogramar; puede ser nulo.
     */
    boolean haySolapamientoDeProfesional(Long profesionalId, Instant inicio, Instant fin, Long excluirTurnoId);

    /** Hay turnos {@code pendiente} o {@code confirmado} del paciente que empiezan después de {@code ahora} (C-15). */
    boolean hayTurnosActivosFuturosDelPaciente(Long pacienteId, Instant ahora);

    /** Ídem {@link #haySolapamientoDeProfesional} para el sillón. */
    boolean haySolapamientoDeSillon(Long sillonId, Instant inicio, Instant fin, Long excluirTurnoId);
}

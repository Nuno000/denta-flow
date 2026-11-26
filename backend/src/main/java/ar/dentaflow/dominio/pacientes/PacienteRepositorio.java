package ar.dentaflow.dominio.pacientes;

import java.util.Optional;

/** Puerto de persistencia de pacientes. Sin borrado (retención: PA-12). */
public interface PacienteRepositorio {

    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorId(Long id);

    /** Igual que {@link #buscarPorId} pero bloquea la fila del paciente hasta el fin de la transacción (C-15). */
    Optional<Paciente> buscarPorIdParaActualizar(Long id);

    /** Busca por DNI ya normalizado (ver {@link Dni#normalizar}). */
    Optional<Paciente> buscarPorDni(String dniNormalizado);
}

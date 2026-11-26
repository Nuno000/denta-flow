package ar.dentaflow.dominio.pacientes;

import java.util.Optional;

/** Puerto de persistencia de pacientes. Sin borrado (retención: PA-12). */
public interface PacienteRepositorio {

    Paciente guardar(Paciente paciente);

    Optional<Paciente> buscarPorId(Long id);

    /** Busca por DNI ya normalizado (ver {@link Dni#normalizar}). */
    Optional<Paciente> buscarPorDni(String dniNormalizado);
}

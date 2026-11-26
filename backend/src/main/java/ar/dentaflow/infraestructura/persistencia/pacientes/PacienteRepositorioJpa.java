package ar.dentaflow.infraestructura.persistencia.pacientes;

import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/** Implementación Spring Data del puerto {@link PacienteRepositorio}; sin {@code delete*} heredados. */
public interface PacienteRepositorioJpa extends Repository<Paciente, Long>, PacienteRepositorio {

    Paciente save(Paciente paciente);

    Optional<Paciente> findById(Long id);

    Optional<Paciente> findByDni(String dni);

    @Override
    default Paciente guardar(Paciente paciente) {
        return save(paciente);
    }

    @Override
    default Optional<Paciente> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default Optional<Paciente> buscarPorDni(String dniNormalizado) {
        return findByDni(dniNormalizado);
    }
}

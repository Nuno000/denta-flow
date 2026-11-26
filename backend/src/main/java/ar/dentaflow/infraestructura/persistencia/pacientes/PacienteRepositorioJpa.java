package ar.dentaflow.infraestructura.persistencia.pacientes;

import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Implementación Spring Data del puerto {@link PacienteRepositorio}; sin {@code delete*} heredados. */
public interface PacienteRepositorioJpa extends Repository<Paciente, Long>, PacienteRepositorio {

    Paciente save(Paciente paciente);

    Optional<Paciente> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.id = :id")
    Optional<Paciente> findByIdParaActualizar(@Param("id") Long id);

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
    default Optional<Paciente> buscarPorIdParaActualizar(Long id) {
        return findByIdParaActualizar(id);
    }

    @Override
    default Optional<Paciente> buscarPorDni(String dniNormalizado) {
        return findByDni(dniNormalizado);
    }
}

package ar.dentaflow.infraestructura.persistencia.recursos;

import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.util.List;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Implementación Spring Data del puerto {@link SillonRepositorio}; sin {@code delete*} heredados. */
public interface SillonRepositorioJpa extends Repository<Sillon, Long>, SillonRepositorio {

    Sillon save(Sillon sillon);

    Optional<Sillon> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sillon s where s.id = :id")
    Optional<Sillon> findParaActualizar(@Param("id") Long id);

    @Override
    default Optional<Sillon> bloquearPorId(Long id) {
        return findParaActualizar(id);
    }

    @Query("select s from Sillon s order by s.id")
    List<Sillon> findTodosPorId();

    @Query("select s from Sillon s where s.activo = true order by s.id")
    List<Sillon> findActivosPorId();

    @Override
    default Sillon guardar(Sillon sillon) {
        return save(sillon);
    }

    @Override
    default Optional<Sillon> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default List<Sillon> listarTodos() {
        return findTodosPorId();
    }

    @Override
    default List<Sillon> listarActivos() {
        return findActivosPorId();
    }
}

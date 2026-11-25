package ar.dentaflow.infraestructura.persistencia.recursos;

import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Implementación Spring Data del puerto {@link SillonRepositorio}; sin {@code delete*} heredados. */
public interface SillonRepositorioJpa extends Repository<Sillon, Long>, SillonRepositorio {

    Sillon save(Sillon sillon);

    Optional<Sillon> findById(Long id);

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

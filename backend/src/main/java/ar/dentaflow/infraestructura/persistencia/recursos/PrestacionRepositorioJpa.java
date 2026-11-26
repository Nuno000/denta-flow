package ar.dentaflow.infraestructura.persistencia.recursos;

import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/**
 * Implementación Spring Data del puerto {@link PrestacionRepositorio}. Extiende {@link Repository}
 * (y no {@code JpaRepository}) para que no exista ningún {@code delete*} heredado.
 */
public interface PrestacionRepositorioJpa extends Repository<Prestacion, Long>, PrestacionRepositorio {

    Prestacion save(Prestacion prestacion);

    /** Guarda y escribe en la base enseguida, así una violación de unicidad aparece en el mismo llamado. */
    Prestacion saveAndFlush(Prestacion prestacion);

    Optional<Prestacion> findById(Long id);

    @Query("select p from Prestacion p order by p.nombre")
    List<Prestacion> findTodasOrdenadas();

    @Query("select p from Prestacion p where p.activa = true order by p.nombre")
    List<Prestacion> findActivasOrdenadas();

    @Override
    default Prestacion guardar(Prestacion prestacion) {
        return saveAndFlush(prestacion);
    }

    @Override
    default Optional<Prestacion> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default List<Prestacion> listarTodas() {
        return findTodasOrdenadas();
    }

    @Override
    default List<Prestacion> listarActivas() {
        return findActivasOrdenadas();
    }
}

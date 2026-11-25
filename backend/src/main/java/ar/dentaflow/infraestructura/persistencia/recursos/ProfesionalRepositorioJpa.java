package ar.dentaflow.infraestructura.persistencia.recursos;

import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Implementación Spring Data del puerto {@link ProfesionalRepositorio}; sin {@code delete*} heredados. */
public interface ProfesionalRepositorioJpa extends Repository<Profesional, Long>, ProfesionalRepositorio {

    Profesional save(Profesional profesional);

    Optional<Profesional> findById(Long id);

    @Query("select p from Profesional p order by p.apellido, p.nombre, p.id")
    List<Profesional> findTodosOrdenados();

    @Query("select p from Profesional p where p.activo = true order by p.apellido, p.nombre, p.id")
    List<Profesional> findActivosOrdenados();

    @Query("select p from Profesional p join p.prestacionIds pid "
            + "where pid = :prestacionId and p.activo = true order by p.id")
    List<Profesional> findActivosPorPrestacion(@Param("prestacionId") Long prestacionId);

    @Override
    default Profesional guardar(Profesional profesional) {
        return save(profesional);
    }

    @Override
    default Optional<Profesional> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default List<Profesional> listarTodos() {
        return findTodosOrdenados();
    }

    @Override
    default List<Profesional> listarActivos() {
        return findActivosOrdenados();
    }

    @Override
    default List<Profesional> listarActivosQueRealizan(Long prestacionId) {
        return findActivosPorPrestacion(prestacionId);
    }
}

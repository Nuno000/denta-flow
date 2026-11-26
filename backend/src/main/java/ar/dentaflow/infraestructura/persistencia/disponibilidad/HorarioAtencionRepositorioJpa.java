package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Implementación Spring Data del puerto {@link HorarioAtencionRepositorio}. */
public interface HorarioAtencionRepositorioJpa extends Repository<HorarioAtencion, Long>, HorarioAtencionRepositorio {

    HorarioAtencion save(HorarioAtencion horario);

    Optional<HorarioAtencion> findById(Long id);

    void delete(HorarioAtencion horario);

    @Query("select h from HorarioAtencion h where h.profesionalId = :profesionalId order by h.dia, h.inicio, h.id")
    List<HorarioAtencion> findDelProfesional(@Param("profesionalId") Long profesionalId);

    @Query("select h from HorarioAtencion h where h.profesionalId = :profesionalId and h.dia = :dia "
            + "order by h.inicio, h.id")
    List<HorarioAtencion> findDelProfesionalYDia(@Param("profesionalId") Long profesionalId,
            @Param("dia") DayOfWeek dia);

    @Override
    default HorarioAtencion guardar(HorarioAtencion horario) {
        return save(horario);
    }

    @Override
    default Optional<HorarioAtencion> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default List<HorarioAtencion> listarDelProfesional(Long profesionalId) {
        return findDelProfesional(profesionalId);
    }

    @Override
    default List<HorarioAtencion> listarDelProfesionalYDia(Long profesionalId, DayOfWeek dia) {
        return findDelProfesionalYDia(profesionalId, dia);
    }

    @Override
    default void eliminar(HorarioAtencion horario) {
        delete(horario);
    }
}

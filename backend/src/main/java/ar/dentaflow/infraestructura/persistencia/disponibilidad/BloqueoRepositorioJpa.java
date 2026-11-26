package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Implementación Spring Data del puerto {@link BloqueoRepositorio}. La superposición con
 * {@code [desde, hasta)} es {@code inicio < hasta AND fin > desde}, que aprovecha los índices
 * {@code (recurso, inicio, fin)} de {@code V006_1}.
 */
public interface BloqueoRepositorioJpa extends Repository<Bloqueo, Long>, BloqueoRepositorio {

    Bloqueo save(Bloqueo bloqueo);

    Optional<Bloqueo> findById(Long id);

    void delete(Bloqueo bloqueo);

    @Query("select b from Bloqueo b where b.inicio < :hasta and b.fin > :desde "
            + "and ((:profesionalId is null and :sillonId is null) "
            + "or b.profesionalId = :profesionalId or b.sillonId = :sillonId) "
            + "order by b.inicio, b.id")
    List<Bloqueo> findSuperpuestos(@Param("desde") Instant desde, @Param("hasta") Instant hasta,
            @Param("profesionalId") Long profesionalId, @Param("sillonId") Long sillonId);

    @Query("select b from Bloqueo b where b.inicio < :hasta and b.fin > :desde "
            + "and (b.ambito = :consultorio or b.profesionalId = :profesionalId) order by b.inicio, b.id")
    List<Bloqueo> findQueAfectanAProfesional(@Param("profesionalId") Long profesionalId,
            @Param("desde") Instant desde, @Param("hasta") Instant hasta,
            @Param("consultorio") AmbitoBloqueo consultorio);

    @Query("select b from Bloqueo b where b.inicio < :hasta and b.fin > :desde "
            + "and (b.ambito = :consultorio or b.sillonId = :sillonId) order by b.inicio, b.id")
    List<Bloqueo> findQueAfectanASillon(@Param("sillonId") Long sillonId,
            @Param("desde") Instant desde, @Param("hasta") Instant hasta,
            @Param("consultorio") AmbitoBloqueo consultorio);

    @Override
    default Bloqueo guardar(Bloqueo bloqueo) {
        return save(bloqueo);
    }

    @Override
    default Optional<Bloqueo> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default void eliminar(Bloqueo bloqueo) {
        delete(bloqueo);
    }

    @Override
    default List<Bloqueo> listarQueSeSuperponen(Instant desde, Instant hasta, Long profesionalId, Long sillonId) {
        return findSuperpuestos(desde, hasta, profesionalId, sillonId);
    }

    @Override
    default List<Bloqueo> listarQueAfectanAProfesional(Long profesionalId, Instant desde, Instant hasta) {
        return findQueAfectanAProfesional(profesionalId, desde, hasta, AmbitoBloqueo.CONSULTORIO);
    }

    @Override
    default List<Bloqueo> listarQueAfectanASillon(Long sillonId, Instant desde, Instant hasta) {
        return findQueAfectanASillon(sillonId, desde, hasta, AmbitoBloqueo.CONSULTORIO);
    }
}

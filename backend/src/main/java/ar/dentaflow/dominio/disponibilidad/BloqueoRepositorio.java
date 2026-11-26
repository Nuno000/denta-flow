package ar.dentaflow.dominio.disponibilidad;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia de los bloqueos. Las consultas por rango usan intervalos semiabiertos
 * {@code [desde, hasta)} y devuelven los bloqueos ordenados por inicio e id.
 */
public interface BloqueoRepositorio {

    Bloqueo guardar(Bloqueo bloqueo);

    Optional<Bloqueo> buscarPorId(Long id);

    void eliminar(Bloqueo bloqueo);

    /**
     * Bloqueos que se superponen con el rango. Con {@code profesionalId} y/o {@code sillonId} no nulos
     * se limitan a los de ese recurso (sin agregar los del consultorio).
     */
    List<Bloqueo> listarQueSeSuperponen(Instant desde, Instant hasta, Long profesionalId, Long sillonId);

    /** Bloqueos del profesional y del consultorio que se superponen con el rango. */
    List<Bloqueo> listarQueAfectanAProfesional(Long profesionalId, Instant desde, Instant hasta);

    /** Bloqueos del sillón y del consultorio que se superponen con el rango. */
    List<Bloqueo> listarQueAfectanASillon(Long sillonId, Instant desde, Instant hasta);
}

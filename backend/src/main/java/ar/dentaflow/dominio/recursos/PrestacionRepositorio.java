package ar.dentaflow.dominio.recursos;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de prestaciones. Sin borrado: las prestaciones se desactivan. */
public interface PrestacionRepositorio {

    Prestacion guardar(Prestacion prestacion);

    Optional<Prestacion> buscarPorId(Long id);

    /** Todas, activas e inactivas, ordenadas por nombre. */
    List<Prestacion> listarTodas();

    /** Solo las activas, ordenadas por nombre. */
    List<Prestacion> listarActivas();
}

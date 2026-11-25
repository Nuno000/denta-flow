package ar.dentaflow.dominio.recursos;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de sillones. Sin borrado: los sillones se desactivan. */
public interface SillonRepositorio {

    Sillon guardar(Sillon sillon);

    Optional<Sillon> buscarPorId(Long id);

    /** Todos, activos e inactivos, por id ascendente. */
    List<Sillon> listarTodos();

    /**
     * Solo los activos, por id ascendente: el mismo orden con el que se bloquean los sillones y se
     * elige "el primer sillón libre".
     */
    List<Sillon> listarActivos();
}

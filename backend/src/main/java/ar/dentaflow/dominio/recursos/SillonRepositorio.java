package ar.dentaflow.dominio.recursos;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de sillones. Sin borrado: los sillones se desactivan. */
public interface SillonRepositorio {

    Sillon guardar(Sillon sillon);

    Optional<Sillon> buscarPorId(Long id);

    /**
     * Lee el sillón y bloquea su fila para escritura hasta el fin de la transacción
     * ({@code SELECT ... FOR UPDATE}). Se toma siempre después del profesional (DD-09). Vacío si no
     * existe.
     */
    Optional<Sillon> bloquearPorId(Long id);

    /** Todos, activos e inactivos, por id ascendente. */
    List<Sillon> listarTodos();

    /**
     * Solo los activos, por id ascendente: el mismo orden con el que se bloquean los sillones y se
     * elige "el primer sillón libre".
     */
    List<Sillon> listarActivos();
}

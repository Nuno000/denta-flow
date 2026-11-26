package ar.dentaflow.dominio.recursos;

import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de profesionales. Sin borrado: los profesionales se desactivan. */
public interface ProfesionalRepositorio {

    Profesional guardar(Profesional profesional);

    Optional<Profesional> buscarPorId(Long id);

    /**
     * Lee el profesional y bloquea su fila para escritura hasta el fin de la transacción
     * ({@code SELECT ... FOR UPDATE}). Orden fijo para evitar interbloqueos: primero el profesional y
     * después el sillón (DD-09). Vacío si no existe.
     */
    Optional<Profesional> bloquearPorId(Long id);

    /** Todos, activos e inactivos, ordenados por apellido y nombre. */
    List<Profesional> listarTodos();

    /** Solo los activos, ordenados por apellido y nombre. */
    List<Profesional> listarActivos();

    /** Profesionales activos que realizan la prestación indicada, por id ascendente. */
    List<Profesional> listarActivosQueRealizan(Long prestacionId);
}

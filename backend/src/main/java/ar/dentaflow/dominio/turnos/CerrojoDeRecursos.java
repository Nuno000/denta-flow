package ar.dentaflow.dominio.turnos;

/**
 * Bloqueo de las filas de profesional y sillón dentro de la transacción en curso (DD-09). El orden es siempre
 * profesional y después sillón. Lanzan {@code RecursoNoEncontradoException} si la fila no existe.
 */
public interface CerrojoDeRecursos {

    void bloquearProfesional(Long profesionalId);

    void bloquearSillon(Long sillonId);
}

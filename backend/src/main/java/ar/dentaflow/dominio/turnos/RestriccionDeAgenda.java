package ar.dentaflow.dominio.turnos;

/**
 * Punto de extensión (design D7): regla de agenda que C-06 y C-10 registran como bean (horarios de atención,
 * bloqueos, RN-TU-04). {@code ServicioDeTurnos} la evalúa con los bloqueos tomados y antes de verificar el
 * solapamiento. C-09 no registra ninguna. Si el turno propuesto no cumple, lanza una excepción de dominio
 * (por ejemplo {@code ValidacionDeDominioException} o {@code ConflictoException}).
 */
public interface RestriccionDeAgenda {

    void verificar(TurnoPropuesto propuesto);
}

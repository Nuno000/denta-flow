package ar.dentaflow.dominio.turnos;

import java.util.Map;

/**
 * Campos que cambiaron en una reprogramación, con su valor anterior y nuevo, listos para auditar
 * ({@code inicio}, {@code fin}, {@code profesional_id}, {@code sillon_id}; instantes en ISO-8601 UTC).
 */
public record CambioDeReprogramacion(Map<String, String> anterior, Map<String, String> nuevo) {

    public CambioDeReprogramacion {
        anterior = Map.copyOf(anterior);
        nuevo = Map.copyOf(nuevo);
    }

    public boolean hayCambios() {
        return !nuevo.isEmpty();
    }
}

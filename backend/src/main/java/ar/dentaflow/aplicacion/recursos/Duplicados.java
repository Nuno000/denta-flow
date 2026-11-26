package ar.dentaflow.aplicacion.recursos;

import ar.dentaflow.dominio.comun.ConflictoException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Detección de duplicados de recursos (design D4). La verificación previa compara sin distinguir mayúsculas
 * ni espacios en los extremos, así MySQL 8 ({@code utf8mb4_0900_ai_ci}) y H2 se comportan igual. La
 * constraint {@code UNIQUE} de la base es el respaldo ante altas simultáneas: su violación se traduce a 409.
 */
final class Duplicados {

    private Duplicados() {
    }

    /**
     * Falla con {@link ConflictoException} si algún otro recurso (id distinto de {@code idPropio}) tiene el
     * mismo valor. Un valor nulo nunca es duplicado.
     */
    static <T> void verificar(Collection<T> existentes, Function<T, Long> id, Function<T, String> valor,
            Long idPropio, String candidato, String mensaje) {
        if (candidato == null) {
            return;
        }
        boolean repetido = existentes.stream()
                .filter(existente -> !Objects.equals(id.apply(existente), idPropio))
                .map(valor)
                .anyMatch(otro -> iguales(otro, candidato));
        if (repetido) {
            throw new ConflictoException(mensaje);
        }
    }

    /**
     * Ejecuta el guardado traduciendo la violación de una constraint conocida al conflicto de su mensaje;
     * cualquier otra violación se propaga.
     */
    static <T> T guardar(Supplier<T> guardado, Map<String, String> mensajesPorConstraint) {
        try {
            return guardado.get();
        } catch (DataIntegrityViolationException e) {
            String detalle = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> entrada : mensajesPorConstraint.entrySet()) {
                if (detalle.contains(entrada.getKey())) {
                    throw new ConflictoException(entrada.getValue());
                }
            }
            throw e;
        }
    }

    private static boolean iguales(String uno, String otro) {
        return uno != null && uno.strip().toLowerCase(Locale.ROOT).equals(otro.strip().toLowerCase(Locale.ROOT));
    }
}

package ar.dentaflow.aplicacion.usuarios;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Límite de ingresos fallidos por email, en memoria (design D7): con {@code intentosMaximos} fallos dentro
 * de una ventana fija que empieza con el primero, bloquea hasta que la ventana vence. Un ingreso correcto
 * reinicia el contador. El estado se pierde al reiniciar el servicio (trade-off aceptado para el MVP).
 */
public class LimitadorDeIntentosDeIngreso {

    private record Ventana(Instant inicio, int fallos) {
    }

    private final Clock reloj;
    private final int intentosMaximos;
    private final Duration duracionVentana;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public LimitadorDeIntentosDeIngreso(Clock reloj, int intentosMaximos, Duration duracionVentana) {
        this.reloj = reloj;
        this.intentosMaximos = intentosMaximos;
        this.duracionVentana = duracionVentana;
    }

    /** Falla con {@link DemasiadosIntentosException} si el email agotó sus intentos en la ventana vigente. */
    public void verificar(String email) {
        Ventana ventana = vigente(email);
        if (ventana != null && ventana.fallos() >= intentosMaximos) {
            throw new DemasiadosIntentosException(Duration.between(reloj.instant(), finDe(ventana)));
        }
    }

    public void registrarFallo(String email) {
        Instant ahora = reloj.instant();
        ventanas.compute(email, (clave, actual) -> actual == null || !ahora.isBefore(finDe(actual))
                ? new Ventana(ahora, 1)
                : new Ventana(actual.inicio(), actual.fallos() + 1));
    }

    public void registrarExito(String email) {
        ventanas.remove(email);
    }

    private Ventana vigente(String email) {
        Ventana ventana = ventanas.get(email);
        if (ventana != null && !reloj.instant().isBefore(finDe(ventana))) {
            ventanas.remove(email, ventana);
            return null;
        }
        return ventana;
    }

    private Instant finDe(Ventana ventana) {
        return ventana.inicio().plus(duracionVentana);
    }
}

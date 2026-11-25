package ar.dentaflow.aplicacion.usuarios;

import java.time.Duration;

/** Se superó el límite de ingresos fallidos para un email (design D7). Se traduce a 429 + Retry-After. */
public class DemasiadosIntentosException extends RuntimeException {

    private final Duration reintentarEn;

    public DemasiadosIntentosException(Duration reintentarEn) {
        super("Demasiados intentos de ingreso. Probá de nuevo más tarde.");
        this.reintentarEn = reintentarEn;
    }

    /** Tiempo hasta que vence la ventana; nunca menos de un segundo. */
    public Duration reintentarEn() {
        return reintentarEn.isNegative() || reintentarEn.isZero() ? Duration.ofSeconds(1) : reintentarEn;
    }
}

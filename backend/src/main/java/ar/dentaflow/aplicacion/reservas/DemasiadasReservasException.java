package ar.dentaflow.aplicacion.reservas;

import java.time.Duration;

/**
 * Un origen superó el límite de pedidos de reserva (C-16 design D5). Se traduce a 429 + {@code Retry-After}. El
 * mensaje no nombra la dirección IP.
 */
public class DemasiadasReservasException extends RuntimeException {

    private final Duration reintentarEn;

    public DemasiadasReservasException(Duration reintentarEn) {
        super("Hiciste demasiados pedidos de reserva. Probá de nuevo en unos minutos.");
        this.reintentarEn = reintentarEn;
    }

    /** Tiempo hasta que vence la ventana; nunca menos de un segundo. */
    public Duration reintentarEn() {
        return reintentarEn.isNegative() || reintentarEn.isZero() ? Duration.ofSeconds(1) : reintentarEn;
    }
}

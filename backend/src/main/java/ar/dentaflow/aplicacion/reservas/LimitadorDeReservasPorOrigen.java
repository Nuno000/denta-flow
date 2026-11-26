package ar.dentaflow.aplicacion.reservas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

/**
 * Límite anti-abuso de la reserva pública (C-16 design D5, spec "Límite de reservas por origen"):
 * {@value #PEDIDOS_MAXIMOS} pedidos por dirección IP en una ventana fija de 10 minutos que empieza con el primero.
 * Cuenta todo pedido, exitoso o no; un pedido rechazado no alarga la ventana. En memoria, como el límite de
 * ingresos de C-04: se pierde al reiniciar y es por instancia (Render gratuito usa una). Nunca loguea la IP.
 */
@Component
public class LimitadorDeReservasPorOrigen {

    public static final int PEDIDOS_MAXIMOS = 5;
    public static final Duration VENTANA = Duration.ofMinutes(10);

    private record Ventana(Instant inicio, int pedidos) {

        Instant fin() {
            return inicio.plus(VENTANA);
        }
    }

    private final Clock reloj;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public LimitadorDeReservasPorOrigen(Clock reloj) {
        this.reloj = reloj;
    }

    /**
     * Cuenta un pedido del origen.
     *
     * @throws DemasiadasReservasException si el origen ya hizo {@value #PEDIDOS_MAXIMOS} pedidos en la ventana
     */
    public void registrarPedido(String origen) {
        Instant ahora = reloj.instant();
        limpiarVencidas(ahora);
        AtomicBoolean rechazado = new AtomicBoolean(false);
        Ventana ventana = ventanas.compute(origen, (clave, actual) -> {
            if (actual == null || !ahora.isBefore(actual.fin())) {
                return new Ventana(ahora, 1);
            }
            if (actual.pedidos() >= PEDIDOS_MAXIMOS) {
                rechazado.set(true);
                return actual;
            }
            return new Ventana(actual.inicio(), actual.pedidos() + 1);
        });
        if (rechazado.get()) {
            throw new DemasiadasReservasException(Duration.between(ahora, ventana.fin()));
        }
    }

    /** Cantidad de orígenes con una ventana vigente (para pruebas y diagnóstico). */
    int origenesRegistrados() {
        limpiarVencidas(reloj.instant());
        return ventanas.size();
    }

    private void limpiarVencidas(Instant ahora) {
        ventanas.entrySet().removeIf(entrada -> !ahora.isBefore(entrada.getValue().fin()));
    }
}

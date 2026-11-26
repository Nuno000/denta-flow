package ar.dentaflow.aplicacion.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Spec reserva-online — "Límite de reservas por origen" (design D5): 5 pedidos por IP cada 10 minutos. */
class LimitadorDeReservasPorOrigenTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");
    private static final String IP = "203.0.113.7";

    /** Reloj que se puede adelantar. */
    private static final class RelojMovil extends Clock {
        private Instant ahora = T0;

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private final RelojMovil reloj = new RelojMovil();
    private final LimitadorDeReservasPorOrigen limitador = new LimitadorDeReservasPorOrigen(reloj);

    private void pedir(int veces, String ip) {
        for (int i = 0; i < veces; i++) {
            limitador.registrarPedido(ip);
        }
    }

    @Test
    void elLimiteEsCincoCadaDiezMinutos() {
        assertThat(LimitadorDeReservasPorOrigen.PEDIDOS_MAXIMOS).isEqualTo(5);
        assertThat(LimitadorDeReservasPorOrigen.VENTANA).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void cincoPedidosPasanYElSextoSeRechazaConLosSegundosRestantes() {
        pedir(5, IP);
        reloj.avanzar(Duration.ofMinutes(3));

        assertThatThrownBy(() -> limitador.registrarPedido(IP))
                .isInstanceOfSatisfying(DemasiadasReservasException.class,
                        e -> assertThat(e.reintentarEn()).isEqualTo(Duration.ofMinutes(7)));
    }

    @Test
    void otraDireccionNoSeVeAfectada() {
        pedir(5, IP);

        assertThatCode(() -> limitador.registrarPedido("198.51.100.1")).doesNotThrowAnyException();
    }

    @Test
    void alVencerLaVentanaVuelveAPasar() {
        pedir(5, IP);
        reloj.avanzar(Duration.ofMinutes(10));

        assertThatCode(() -> pedir(5, IP)).doesNotThrowAnyException();
        assertThatThrownBy(() -> limitador.registrarPedido(IP)).isInstanceOf(DemasiadasReservasException.class);
    }

    @Test
    void lasVentanasVencidasSeLimpian() {
        pedir(1, IP);
        pedir(1, "198.51.100.1");
        reloj.avanzar(Duration.ofMinutes(11));

        limitador.registrarPedido("192.0.2.9");

        assertThat(limitador.origenesRegistrados()).isEqualTo(1);
    }

    @Test
    void unRechazoNoAlargaLaVentana() {
        pedir(5, IP);
        assertThatThrownBy(() -> limitador.registrarPedido(IP)).isInstanceOf(DemasiadasReservasException.class);
        reloj.avanzar(Duration.ofMinutes(10));

        assertThatCode(() -> limitador.registrarPedido(IP)).doesNotThrowAnyException();
    }

    @Test
    void elMensajeNoIncluyeLaDireccion() {
        pedir(5, IP);

        assertThatThrownBy(() -> limitador.registrarPedido(IP)).hasMessageNotContaining(IP);
    }
}

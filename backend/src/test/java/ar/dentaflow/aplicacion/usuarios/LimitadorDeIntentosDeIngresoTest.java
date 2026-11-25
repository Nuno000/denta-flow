package ar.dentaflow.aplicacion.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** Spec autenticacion-personal — "Límite de intentos fallidos" (design D7). */
class LimitadorDeIntentosDeIngresoTest {

    /** Reloj que la prueba adelanta a mano. */
    static final class RelojMutable extends Clock {
        private Instant ahora = Instant.parse("2026-11-25T13:00:00Z");

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

    private final RelojMutable reloj = new RelojMutable();
    private final LimitadorDeIntentosDeIngreso limitador =
            new LimitadorDeIntentosDeIngreso(reloj, 5, Duration.ofMinutes(15));

    private void fallar(String email, int veces) {
        for (int i = 0; i < veces; i++) {
            limitador.registrarFallo(email);
            reloj.avanzar(Duration.ofSeconds(10));
        }
    }

    @Test
    void cuatroFallosNoBloquean() {
        fallar("a@example.com", 4);

        assertThatCode(() -> limitador.verificar("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void conCincoFallosEnLaVentanaBloqueaIndicandoCuantoFalta() {
        fallar("a@example.com", 5);

        assertThatThrownBy(() -> limitador.verificar("a@example.com"))
                .isInstanceOf(DemasiadosIntentosException.class)
                .satisfies(e -> assertThat(((DemasiadosIntentosException) e).reintentarEn())
                        .isEqualTo(Duration.ofMinutes(15).minusSeconds(50)));
    }

    @Test
    void alVencerLaVentanaSeLibera() {
        fallar("a@example.com", 5);
        reloj.avanzar(Duration.ofMinutes(15));

        assertThatCode(() -> limitador.verificar("a@example.com")).doesNotThrowAnyException();
        limitador.registrarFallo("a@example.com");
        assertThatCode(() -> limitador.verificar("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void unIngresoCorrectoReiniciaElContador() {
        fallar("a@example.com", 3);
        limitador.registrarExito("a@example.com");
        fallar("a@example.com", 4);

        assertThatCode(() -> limitador.verificar("a@example.com")).doesNotThrowAnyException();
    }

    @Test
    void emailsDistintosNoSeAfectan() {
        fallar("a@example.com", 5);

        assertThatCode(() -> limitador.verificar("b@example.com")).doesNotThrowAnyException();
    }
}

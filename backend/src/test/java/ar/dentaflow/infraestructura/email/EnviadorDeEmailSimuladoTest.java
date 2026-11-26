package ar.dentaflow.infraestructura.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.notificaciones.FallaDeEnvio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import org.junit.jupiter.api.Test;

/** Spec notificaciones-email — "Canal desacoplado del proveedor": canal simulado en memoria (design D4). */
class EnviadorDeEmailSimuladoTest {

    private final EnviadorDeEmailSimulado enviador = new EnviadorDeEmailSimulado();

    private static MensajeEmail mensaje(String destinatario) {
        return new MensajeEmail(destinatario, "Asunto", "texto", "<p>html</p>");
    }

    @Test
    void guardaLosMensajesEnviados() {
        enviador.enviar(mensaje("a@example.com"));
        enviador.enviar(mensaje("b@example.com"));

        assertThat(enviador.mensajes()).extracting(MensajeEmail::destinatario)
                .containsExactly("a@example.com", "b@example.com");
        assertThat(enviador.mensajesPara("b@example.com")).hasSize(1);
    }

    @Test
    void conservaSoloLosUltimos50() {
        for (int i = 0; i < 55; i++) {
            enviador.enviar(mensaje("p" + i + "@example.com"));
        }

        assertThat(enviador.mensajes()).hasSize(EnviadorDeEmailSimulado.MAXIMO_GUARDADOS);
        assertThat(enviador.mensajes().get(0).destinatario()).isEqualTo("p5@example.com");
    }

    @Test
    void sePuedeConfigurarParaFallarYDespuesEnviar() {
        enviador.fallarLasProximas(1, MotivoFallo.CONEXION);

        assertThatThrownBy(() -> enviador.enviar(mensaje("a@example.com")))
                .isInstanceOfSatisfying(FallaDeEnvio.class, f -> assertThat(f.motivo()).isEqualTo(MotivoFallo.CONEXION));
        enviador.enviar(mensaje("a@example.com"));

        assertThat(enviador.mensajes()).hasSize(1);
        assertThat(enviador.intentos()).isEqualTo(2);
    }

    @Test
    void limpiarBorraMensajesYFallasPendientes() {
        enviador.fallarLasProximas(5, MotivoFallo.CONEXION);
        enviador.limpiar();

        enviador.enviar(mensaje("a@example.com"));
        assertThat(enviador.mensajes()).hasSize(1);
        assertThat(enviador.intentos()).isEqualTo(1);
    }
}

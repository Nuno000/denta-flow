package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Spec notificaciones-email — "Email de confirmación de reserva" (design D4). */
class PlantillaConfirmacionReservaTest {

    private static final String BASE = "https://turnos.example/turno/TOKEN43/";
    private static final EnlacesDelTurno ENLACES =
            new EnlacesDelTurno(BASE + "confirmar", BASE + "cancelar", BASE + "reprogramar");

    private final PlantillaConfirmacionReserva plantilla = new PlantillaConfirmacionReserva();

    private MensajeEmail armar(String nombre) {
        return plantilla.armar(new DatosDelEmail("paciente@example.com", nombre,
                Instant.parse("2026-12-02T13:00:00Z"), "Limpieza", "Ana Pérez", ENLACES));
    }

    @Test
    void muestraLaHoraDelConsultorioLaPrestacionElProfesionalYLosEnlaces() {
        MensajeEmail mensaje = armar("Carla");

        assertThat(mensaje.destinatario()).isEqualTo("paciente@example.com");
        assertThat(mensaje.asunto()).contains("02/12/2026 10:00");
        for (String cuerpo : new String[] {mensaje.textoPlano(), mensaje.html()}) {
            assertThat(cuerpo).contains("02/12/2026 10:00", "Limpieza", "Ana Pérez", "Carla", BASE + "confirmar",
                    BASE + "cancelar", BASE + "reprogramar");
        }
    }

    @Test
    void noIncluyeDniNiTelefono() {
        MensajeEmail mensaje = armar("Carla");

        String todo = mensaje.asunto() + mensaje.textoPlano() + mensaje.html();
        assertThat(todo).doesNotContain("30111222").doesNotContain("1155550000").doesNotContainIgnoringCase("dni");
    }

    @Test
    void escapaElContenidoEnLaVersionHtml() {
        MensajeEmail mensaje = armar("<b>Ana</b> & \"Cía\" 'x'");

        assertThat(mensaje.html()).contains("&lt;b&gt;Ana&lt;/b&gt; &amp; &quot;Cía&quot; &#39;x&#39;")
                .doesNotContain("<b>Ana</b>");
        assertThat(mensaje.textoPlano()).contains("<b>Ana</b>");
    }

    @Test
    void elToStringDelMensajeNoMuestraDestinatarioNiCuerpo() {
        String texto = armar("Carla").toString();

        assertThat(texto).doesNotContain("paciente@example.com").doesNotContain("Carla").doesNotContain("TOKEN43");
    }
}

package ar.dentaflow.dominio.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** C-21 design D3, spec mensajes-whatsapp "Mensaje desde la plantilla con marcadores". */
class PlantillaWhatsappTest {

    private static final DatosDelMensajeWhatsapp DATOS = new DatosDelMensajeWhatsapp(
            "Ana", Instant.parse("2026-12-05T02:30:00Z"), "Limpieza dental", "Laura Ejemplo",
            "https://dentaflow.test/turno/abc/confirmar");

    @Test
    void reemplazaLosSeisMarcadores() {
        String plantilla = "Hola {nombre}: {prestacion} con {profesional} el {fecha} a las {hora}. Confirmá en {enlace}";

        assertThat(PlantillaWhatsapp.completar(plantilla, DATOS)).isEqualTo(
                "Hola Ana: Limpieza dental con Laura Ejemplo el 04/12/2026 a las 23:30. "
                        + "Confirmá en https://dentaflow.test/turno/abc/confirmar");
    }

    @Test
    void fechaYHoraEnLaZonaDelConsultorio() {
        assertThat(PlantillaWhatsapp.completar("{fecha} {hora}", DATOS)).isEqualTo("04/12/2026 23:30");
    }

    @Test
    void marcadorDesconocidoQuedaLiteral() {
        assertThat(PlantillaWhatsapp.completar("Hola {nombre}, {saludo}", DATOS)).isEqualTo("Hola Ana, {saludo}");
    }

    @Test
    void marcadorRepetidoSeReemplazaTodasLasVeces() {
        assertThat(PlantillaWhatsapp.completar("{nombre} {nombre}", DATOS)).isEqualTo("Ana Ana");
    }

    @Test
    void unValorConLlavesNoSeVuelveAInterpretar() {
        DatosDelMensajeWhatsapp conLlaves = new DatosDelMensajeWhatsapp("{hora}", DATOS.inicio(), "P", "Q", "E");

        assertThat(PlantillaWhatsapp.completar("{nombre}-{hora}", conLlaves)).isEqualTo("{hora}-23:30");
    }
}

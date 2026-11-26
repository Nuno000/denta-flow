package ar.dentaflow.dominio.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** C-21 design D4, spec mensajes-whatsapp "Mensaje codificado en el enlace". */
class EnlaceWhatsappTest {

    @Test
    void armaElEnlaceWaMeConElTextoCodificado() {
        assertThat(EnlaceWhatsapp.de("5492615550000", "Hola Ana"))
                .isEqualTo("https://wa.me/5492615550000?text=Hola%20Ana");
    }

    @Test
    void codificaAcentosYCaracteresReservados() {
        assertThat(EnlaceWhatsapp.de("5491155550000", "¿Venís? Turno #2 & más"))
                .isEqualTo("https://wa.me/5491155550000?text=%C2%BFVen%C3%ADs%3F%20Turno%20%232%20%26%20m%C3%A1s");
    }

    @Test
    void unMasLiteralQuedaCodificadoYNoComoEspacio() {
        assertThat(EnlaceWhatsapp.de("5491155550000", "1+1"))
                .isEqualTo("https://wa.me/5491155550000?text=1%2B1");
    }

    @Test
    void conservaLosSaltosDeLinea() {
        assertThat(EnlaceWhatsapp.de("5491155550000", "a\nb"))
                .isEqualTo("https://wa.me/5491155550000?text=a%0Ab");
    }
}

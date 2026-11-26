package ar.dentaflow.dominio.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** C-21 design D2, spec mensajes-whatsapp "Teléfono normalizado al formato argentino de WhatsApp". */
class TelefonoWhatsappTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(delimiter = '|', value = {
            "011 15 5555-0000      | 5491155550000",
            "1155550000            | 5491155550000",
            "0261 15 555-0000      | 5492615550000",
            "2615550000            | 5492615550000",
            "(0294) 15-455-5000    | 5492944555000",
            "+542615550000         | 5492615550000",
            "+5492954555000        | 5492954555000",
            "0054 9 11 5555 0000   | 5491155550000",
            "+54 11 15 5555 0000   | 5491155550000",
            "351.155.550.000       | 5493515550000"
    })
    void normalizaTelefonosArgentinosAlFormatoDeWhatsapp(String telefono, String esperado) {
        assertThat(TelefonoWhatsapp.normalizar(telefono)).contains(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "+59899123456",     // Uruguay
            "00000000",         // paciente anonimizado (C-15)
            "12345",            // muy corto
            "261155550001234",  // muy largo
            "026155555500",     // 11 dígitos nacionales sin 15
            "2614445550000",    // 13 dígitos nacionales
            "1111111111",       // un solo dígito repetido
            "+5492615550",      // internacional incompleto
            "abc"
    })
    void rechazaTelefonosQueNoSonCelularesArgentinosUtilizables(String telefono) {
        assertThat(TelefonoWhatsapp.normalizar(telefono)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rechazaTelefonoNuloOVacio(String telefono) {
        assertThat(TelefonoWhatsapp.normalizar(telefono)).isEmpty();
    }
}

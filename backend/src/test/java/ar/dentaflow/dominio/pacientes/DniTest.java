package ar.dentaflow.dominio.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec pacientes — "DNI normalizado" (design D3). */
class DniTest {

    @ParameterizedTest
    @ValueSource(strings = {"30.123.456", "30 123 456", "30-123-456", " 30123456 "})
    void normalizaPuntosEspaciosYGuiones(String escrito) {
        assertThat(Dni.normalizar(escrito)).isEqualTo("30123456");
    }

    @Test
    void aceptaSieteDigitos() {
        assertThat(Dni.normalizar("1.234.567")).isEqualTo("1234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"30A23456", "123456", "123456789", "12.34", "abcdefgh", "   "})
    @NullAndEmptySource
    void rechazaLoQueNoSonSieteOOchoDigitos(String escrito) {
        assertThatThrownBy(() -> Dni.normalizar(escrito))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("DNI");
    }
}

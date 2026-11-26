package ar.dentaflow.infraestructura.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Spec turnos — "Token de acceso" (design D9): 256 bits, Base64 URL sin relleno. */
class GeneradorDeTokensSeguroTest {

    @Test
    void generaMilTokensDistintosDe43CaracteresUrlSeguros() {
        GeneradorDeTokensSeguro generador = new GeneradorDeTokensSeguro();
        Set<String> tokens = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            tokens.add(generador.nuevoToken());
        }

        assertThat(tokens).hasSize(1000);
        assertThat(tokens).allSatisfy(token -> assertThat(token).matches("[A-Za-z0-9_-]{43}"));
    }

    @Test
    void dosGeneradoresNoProducenLaMismaSecuencia() {
        assertThat(new GeneradorDeTokensSeguro().nuevoToken()).isNotEqualTo(new GeneradorDeTokensSeguro().nuevoToken());
    }
}

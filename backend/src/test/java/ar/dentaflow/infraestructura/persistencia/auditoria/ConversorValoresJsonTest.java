package ar.dentaflow.infraestructura.persistencia.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec "Valores anterior y nuevo legibles" (design D3): texto JSON estable, sin truncar nunca. */
class ConversorValoresJsonTest {

    private final ConversorValoresJson conversor = new ConversorValoresJson();

    @Test
    void guardaUnCampoComoJsonLegible() {
        assertThat(conversor.convertToDatabaseColumn(Map.of("estado", "pendiente")))
                .isEqualTo("{\"estado\":\"pendiente\"}");
    }

    @Test
    void respetaElOrdenDeLosCamposEnIdaYVuelta() {
        Map<String, String> valores = new LinkedHashMap<>();
        valores.put("inicio", "2026-12-02T15:30:00Z");
        valores.put("fin", "2026-12-02T16:00:00Z");
        valores.put("es_sobreturno", "true");

        String json = conversor.convertToDatabaseColumn(valores);

        assertThat(json).isEqualTo(
                "{\"inicio\":\"2026-12-02T15:30:00Z\",\"fin\":\"2026-12-02T16:00:00Z\",\"es_sobreturno\":\"true\"}");
        assertThat(conversor.convertToEntityAttribute(json).keySet())
                .containsExactly("inicio", "fin", "es_sobreturno");
    }

    @Test
    void sinValoresSeGuardaNulo() {
        assertThat(conversor.convertToDatabaseColumn(null)).isNull();
        assertThat(conversor.convertToDatabaseColumn(Map.of())).isNull();
        assertThat(conversor.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void comillasYTildesVuelvenIguales() {
        Map<String, String> valores = Map.of("motivo", "Llamó: \"no puede\" \\ mañana");

        String json = conversor.convertToDatabaseColumn(valores);

        assertThat(conversor.convertToEntityAttribute(json)).isEqualTo(valores);
    }

    @Test
    void rechazaValoresQueNoEntranEnLaColumnaEnVezDeTruncarlos() {
        Map<String, String> enElLimite = Map.of("x", "a".repeat(4000 - "{\"x\":\"\"}".length()));
        Map<String, String> demasiado = Map.of("x", "a".repeat(4000));

        assertThat(conversor.convertToDatabaseColumn(enElLimite)).hasSize(4000);
        assertThatThrownBy(() -> conversor.convertToDatabaseColumn(demasiado))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("4000");
    }
}

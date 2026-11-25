package ar.dentaflow.dominio.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec "Registro de quién y cuándo" y "Valores anterior y nuevo legibles", a nivel de dominio. */
class CambioDeTurnoTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T13:15:00Z");

    @Test
    void conservaTodoLoQueSeRegistra() {
        CambioDeTurno cambio = CambioDeTurno.registrar(42L, Actor.usuario(7L), AHORA, AccionDeTurno.CAMBIO_ESTADO,
                Map.of("estado", "pendiente"), Map.of("estado", "confirmado"));

        assertThat(cambio.id()).isNull();
        assertThat(cambio.turnoId()).isEqualTo(42L);
        assertThat(cambio.actor()).isEqualTo(Actor.usuario(7L));
        assertThat(cambio.ocurridoEn()).isEqualTo(AHORA);
        assertThat(cambio.accion()).isEqualTo(AccionDeTurno.CAMBIO_ESTADO);
        assertThat(cambio.valorAnterior()).containsExactly(Map.entry("estado", "pendiente"));
        assertThat(cambio.valorNuevo()).containsExactly(Map.entry("estado", "confirmado"));
    }

    @Test
    void laCreacionAdmiteValorAnteriorVacio() {
        CambioDeTurno conNulo = CambioDeTurno.registrar(43L, Actor.paciente(), AHORA, AccionDeTurno.CREACION,
                null, Map.of("estado", "pendiente"));
        CambioDeTurno conVacio = CambioDeTurno.registrar(43L, Actor.sistema(), AHORA, AccionDeTurno.CREACION,
                Map.of(), Map.of("estado", "pendiente"));

        assertThat(conNulo.valorAnterior()).isEmpty();
        assertThat(conVacio.valorAnterior()).isEmpty();
    }

    @Test
    void otraAccionNoAdmiteValorAnteriorVacio() {
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.usuario(7L), AHORA, AccionDeTurno.CANCELACION,
                        Map.of(), Map.of("estado", "cancelado")))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("anterior");
    }

    @Test
    void unCambioSinValoresSeRechaza() {
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.usuario(7L), AHORA, AccionDeTurno.CAMBIO_ESTADO,
                        null, null))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.sistema(), AHORA, AccionDeTurno.CREACION,
                        Map.of(), Map.of()))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void losDatosObligatoriosSeValidan() {
        Map<String, String> nuevo = Map.of("estado", "confirmado");
        Map<String, String> anterior = Map.of("estado", "pendiente");

        assertThatThrownBy(() -> CambioDeTurno.registrar(0L, Actor.sistema(), AHORA, AccionDeTurno.CONFIRMACION,
                anterior, nuevo)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CambioDeTurno.registrar(null, Actor.sistema(), AHORA, AccionDeTurno.CONFIRMACION,
                anterior, nuevo)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, null, AHORA, AccionDeTurno.CONFIRMACION,
                anterior, nuevo)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.sistema(), null, AccionDeTurno.CONFIRMACION,
                anterior, nuevo)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.sistema(), AHORA, null,
                anterior, nuevo)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void lasClavesEnBlancoOValoresNulosSeRechazan() {
        Map<String, String> conValorNulo = new LinkedHashMap<>();
        conValorNulo.put("estado", null);

        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.sistema(), AHORA, AccionDeTurno.CREACION,
                null, Map.of(" ", "x"))).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CambioDeTurno.registrar(42L, Actor.sistema(), AHORA, AccionDeTurno.CREACION,
                null, conValorNulo)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void losValoresRespetanElOrdenYNoSePuedenModificar() {
        Map<String, String> nuevo = new LinkedHashMap<>();
        nuevo.put("inicio", "2026-12-02T15:30:00Z");
        nuevo.put("fin", "2026-12-02T16:00:00Z");
        nuevo.put("es_sobreturno", "true");

        CambioDeTurno cambio = CambioDeTurno.registrar(44L, Actor.usuario(7L), AHORA, AccionDeTurno.CREACION,
                null, nuevo);
        nuevo.put("estado", "pendiente");

        assertThat(cambio.valorNuevo().keySet()).containsExactly("inicio", "fin", "es_sobreturno");
        assertThatThrownBy(() -> cambio.valorNuevo().put("estado", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> cambio.valorAnterior().put("estado", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

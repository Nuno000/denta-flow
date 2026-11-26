package ar.dentaflow.dominio.turnos;

import static ar.dentaflow.dominio.turnos.EstadoTurno.ASISTIO;
import static ar.dentaflow.dominio.turnos.EstadoTurno.AUSENTE;
import static ar.dentaflow.dominio.turnos.EstadoTurno.AUSENTE_CON_AVISO;
import static ar.dentaflow.dominio.turnos.EstadoTurno.CANCELADO;
import static ar.dentaflow.dominio.turnos.EstadoTurno.CONFIRMADO;
import static ar.dentaflow.dominio.turnos.EstadoTurno.PENDIENTE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Spec turnos — "Transiciones de estado explícitas" (design D4, Q6): las 36 combinaciones de la tabla. */
class EstadoTurnoTest {

    /** Orden de columnas: pendiente, confirmado, cancelado, asistio, ausente, ausente_con_aviso. */
    private static final Map<EstadoTurno, boolean[]> TABLA = Map.of(
            PENDIENTE, new boolean[] {true, true, true, true, true, true},
            CONFIRMADO, new boolean[] {false, true, true, true, true, true},
            CANCELADO, new boolean[] {false, false, true, false, false, false},
            ASISTIO, new boolean[] {false, false, false, true, true, true},
            AUSENTE, new boolean[] {false, false, false, true, true, true},
            AUSENTE_CON_AVISO, new boolean[] {false, false, false, true, true, true});

    private static final EstadoTurno[] ORDEN = {PENDIENTE, CONFIRMADO, CANCELADO, ASISTIO, AUSENTE,
            AUSENTE_CON_AVISO};

    static Stream<Arguments> combinaciones() {
        Stream.Builder<Arguments> todas = Stream.builder();
        for (EstadoTurno desde : ORDEN) {
            for (int i = 0; i < ORDEN.length; i++) {
                todas.add(Arguments.of(desde, ORDEN[i], TABLA.get(desde)[i]));
            }
        }
        return todas.build();
    }

    @ParameterizedTest(name = "{0} -> {1} permitido={2}")
    @MethodSource("combinaciones")
    void respetaLaTablaDeTransiciones(EstadoTurno desde, EstadoTurno hacia, boolean permitido) {
        assertThat(desde.puedePasarA(hacia)).isEqualTo(permitido);
    }

    @Test
    void sonLasTreintaYSeisCombinaciones() {
        assertThat(combinaciones().count()).isEqualTo(36);
    }

    @Test
    void loscodigosSonLosDeLaBase() {
        assertThat(EstadoTurno.values()).extracting(EstadoTurno::codigo).containsExactly(
                "pendiente", "confirmado", "cancelado", "asistio", "ausente", "ausente_con_aviso");
    }

    @ParameterizedTest
    @MethodSource("todos")
    void desdeCodigoEsLaInversaDeCodigo(EstadoTurno estado) {
        assertThat(EstadoTurno.desdeCodigo(estado.codigo())).isEqualTo(estado);
    }

    static Stream<EstadoTurno> todos() {
        return Stream.of(EstadoTurno.values());
    }

    @Test
    void rechazaUnCodigoDesconocido() {
        assertThatThrownBy(() -> EstadoTurno.desdeCodigo("borrado")).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> EstadoTurno.desdeCodigo(null)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @ParameterizedTest
    @MethodSource("todos")
    void soloElCanceladoLiberaLosRecursos(EstadoTurno estado) {
        assertThat(estado.ocupaRecursos()).isEqualTo(estado != CANCELADO);
    }

    @Test
    void origenesConSuCodigo() {
        assertThat(OrigenTurno.desdeCodigo("online")).isEqualTo(OrigenTurno.ONLINE);
        assertThat(OrigenTurno.desdeCodigo("recepcion")).isEqualTo(OrigenTurno.RECEPCION);
        assertThat(OrigenTurno.RECEPCION.codigo()).isEqualTo("recepcion");
        assertThatThrownBy(() -> OrigenTurno.desdeCodigo("telefono"))
                .isInstanceOf(ValidacionDeDominioException.class);
    }
}

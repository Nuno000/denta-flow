package ar.dentaflow.dominio.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Spec enlaces-paciente — "Resolución de un token" (design D6). */
class SituacionDeEnlaceTest {

    private static final Instant INICIO = Instant.parse("2026-12-02T13:00:00Z");
    private static final Instant ANTES = Instant.parse("2026-12-01T13:00:00Z");
    private static final String TOKEN = "a".repeat(43);

    private Turno turno(EstadoTurno estado) {
        Turno turno = Turno.nuevo(1L, 2L, 3L, 4L, INICIO, 30, OrigenTurno.ONLINE, false, null,
                Instant.parse("2026-11-30T12:00:00Z"), TOKEN);
        if (estado != EstadoTurno.PENDIENTE) {
            if (estado != EstadoTurno.CONFIRMADO && estado != EstadoTurno.CANCELADO) {
                turno.cambiarEstado(EstadoTurno.CONFIRMADO);
            }
            turno.cambiarEstado(estado);
        }
        return turno;
    }

    @Test
    void pendienteFuturoEsVigente() {
        assertThat(SituacionDeEnlace.de(turno(EstadoTurno.PENDIENTE), ANTES)).isEqualTo(SituacionDeEnlace.VIGENTE);
    }

    @Test
    void confirmadoFuturoEsVigente() {
        assertThat(SituacionDeEnlace.de(turno(EstadoTurno.CONFIRMADO), ANTES)).isEqualTo(SituacionDeEnlace.VIGENTE);
    }

    @Test
    void canceladoEsCancelado() {
        assertThat(SituacionDeEnlace.de(turno(EstadoTurno.CANCELADO), ANTES)).isEqualTo(SituacionDeEnlace.CANCELADO);
    }

    @Test
    void canceladoSigueCanceladoAunqueYaHayaPasado() {
        assertThat(SituacionDeEnlace.de(turno(EstadoTurno.CANCELADO), INICIO.plusSeconds(3600)))
                .isEqualTo(SituacionDeEnlace.CANCELADO);
    }

    @Test
    void cuandoLlegaElInicioVence() {
        assertThat(SituacionDeEnlace.de(turno(EstadoTurno.PENDIENTE), INICIO)).isEqualTo(SituacionDeEnlace.VENCIDO);
        assertThat(SituacionDeEnlace.de(turno(EstadoTurno.PENDIENTE), INICIO.minusNanos(1000)))
                .isEqualTo(SituacionDeEnlace.VIGENTE);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"ASISTIO", "AUSENTE", "AUSENTE_CON_AVISO"})
    void conAsistenciaRegistradaVence(EstadoTurno estado) {
        assertThat(SituacionDeEnlace.de(turno(estado), ANTES)).isEqualTo(SituacionDeEnlace.VENCIDO);
    }
}

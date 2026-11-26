package ar.dentaflow.dominio.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Spec turnos — alta, estados y reprogramación del turno (design D4). */
class TurnoTest {

    private static final Instant INICIO = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");
    private static final String TOKEN = "t".repeat(43);

    private static Turno online() {
        return Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.ONLINE, false, null, AHORA, TOKEN);
    }

    private static Turno recepcion() {
        return Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.RECEPCION, false, 9L, AHORA, TOKEN);
    }

    // --- Alta ---

    @Test
    void altaCalculaElFinYNaceEnPendienteEnAmbosOrigenes() {
        for (Turno turno : new Turno[] {online(), recepcion()}) {
            assertThat(turno.getInicio()).isEqualTo(INICIO);
            assertThat(turno.getFin()).isEqualTo(Instant.parse("2026-12-01T12:30:00Z"));
            assertThat(turno.getEstado()).isEqualTo(EstadoTurno.PENDIENTE);
            assertThat(turno.getPacienteId()).isEqualTo(7L);
            assertThat(turno.getProfesionalId()).isEqualTo(3L);
            assertThat(turno.getSillonId()).isEqualTo(2L);
            assertThat(turno.getPrestacionId()).isEqualTo(5L);
            assertThat(turno.getCreadoEn()).isEqualTo(AHORA);
            assertThat(turno.tokenAcceso()).isEqualTo(TOKEN);
        }
    }

    @Test
    void altaConOtraDuracionCalculaOtroFin() {
        Turno turno = Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 45, OrigenTurno.ONLINE, false, null, AHORA, TOKEN);

        assertThat(turno.getFin()).isEqualTo(Instant.parse("2026-12-01T12:45:00Z"));
    }

    @Test
    void rechazaInicioConSegundosONanos() {
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO.plusSeconds(1), 30, OrigenTurno.ONLINE, false,
                null, AHORA, TOKEN)).isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("minuto");
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO.plusNanos(1), 30, OrigenTurno.ONLINE, false,
                null, AHORA, TOKEN)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void rechazaDuracionNoPositivaYDatosNulos() {
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 0, OrigenTurno.ONLINE, false, null, AHORA, TOKEN))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Turno.nuevo(null, 3L, 2L, 5L, INICIO, 30, OrigenTurno.ONLINE, false, null, AHORA,
                TOKEN)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.ONLINE, false, null, AHORA, " "))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void onlineConCreadorYRecepcionSinCreadorSeRechazan() {
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.ONLINE, false, 9L, AHORA, TOKEN))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.RECEPCION, false, null, AHORA,
                TOKEN)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void sobreturnoOnlineSeRechazaYElDeRecepcionSeAcepta() {
        assertThatThrownBy(() -> Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.ONLINE, true, null, AHORA, TOKEN))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("sobreturno");

        Turno sobreturno = Turno.nuevo(7L, 3L, 2L, 5L, INICIO, 30, OrigenTurno.RECEPCION, true, 9L, AHORA, TOKEN);
        assertThat(sobreturno.isEsSobreturno()).isTrue();
        assertThat(sobreturno.getCreadoPorUsuarioId()).isEqualTo(9L);
    }

    @Test
    void valoresDeAltaSonAuditablesYNoLlevanElToken() {
        Map<String, String> valores = recepcion().valoresDeAlta();

        assertThat(valores).containsOnly(
                Map.entry("paciente_id", "7"),
                Map.entry("profesional_id", "3"),
                Map.entry("sillon_id", "2"),
                Map.entry("prestacion_id", "5"),
                Map.entry("inicio", "2026-12-01T12:00:00Z"),
                Map.entry("fin", "2026-12-01T12:30:00Z"),
                Map.entry("estado", "pendiente"),
                Map.entry("origen", "recepcion"),
                Map.entry("es_sobreturno", "false"));
        assertThat(valores.values()).doesNotContain(TOKEN);
    }

    @Test
    void toStringNoExponeElToken() {
        assertThat(online().toString()).doesNotContain(TOKEN);
    }

    // --- Estados ---

    @Test
    void cambiarEstadoValidoDevuelveAnteriorYNuevo() {
        Turno turno = online();

        CambioDeEstado cambio = turno.cambiarEstado(EstadoTurno.CONFIRMADO);

        assertThat(cambio.hayCambio()).isTrue();
        assertThat(cambio.anterior()).isEqualTo(EstadoTurno.PENDIENTE);
        assertThat(cambio.nuevo()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(turno.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
    }

    @Test
    void cambiarAlMismoEstadoEsUnCambioVacioQueNoModificaNada() {
        Turno turno = online();
        turno.cambiarEstado(EstadoTurno.CONFIRMADO);

        CambioDeEstado cambio = turno.cambiarEstado(EstadoTurno.CONFIRMADO);

        assertThat(cambio.hayCambio()).isFalse();
        assertThat(turno.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
    }

    @Test
    void unaTransicionInvalidaLanzaYDejaElEstadoIgual() {
        Turno turno = online();
        turno.cambiarEstado(EstadoTurno.CANCELADO);

        assertThatThrownBy(() -> turno.cambiarEstado(EstadoTurno.CONFIRMADO))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class)
                .isInstanceOf(OperacionNoPermitidaException.class)
                .hasMessageContaining("cancelado").hasMessageContaining("confirmado");
        assertThat(turno.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
    }

    @Test
    void cambiarEstadoANuloSeRechaza() {
        assertThatThrownBy(() -> online().cambiarEstado(null)).isInstanceOf(ValidacionDeDominioException.class);
    }

    // --- Reprogramación ---

    @Test
    void reprogramarDesdePendienteOConfirmadoCambiaSoloLoQueCorresponde() {
        for (EstadoTurno estado : new EstadoTurno[] {EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO}) {
            Turno turno = online();
            turno.cambiarEstado(estado);
            Instant nuevoInicio = Instant.parse("2026-12-02T15:30:00Z");

            CambioDeReprogramacion cambio = turno.reprogramar(nuevoInicio, 30, 3L, 4L);

            assertThat(turno.getInicio()).isEqualTo(nuevoInicio);
            assertThat(turno.getFin()).isEqualTo(Instant.parse("2026-12-02T16:00:00Z"));
            assertThat(turno.getSillonId()).isEqualTo(4L);
            assertThat(turno.getProfesionalId()).isEqualTo(3L);
            assertThat(turno.getEstado()).isEqualTo(estado);
            assertThat(turno.getPacienteId()).isEqualTo(7L);
            assertThat(turno.tokenAcceso()).isEqualTo(TOKEN);
            assertThat(cambio.anterior()).containsOnly(
                    Map.entry("inicio", "2026-12-01T12:00:00Z"),
                    Map.entry("fin", "2026-12-01T12:30:00Z"),
                    Map.entry("sillon_id", "2"));
            assertThat(cambio.nuevo()).containsOnly(
                    Map.entry("inicio", "2026-12-02T15:30:00Z"),
                    Map.entry("fin", "2026-12-02T16:00:00Z"),
                    Map.entry("sillon_id", "4"));
        }
    }

    @Test
    void reprogramarProfesionalYDuracionDistintaIncluyeEsosCampos() {
        Turno turno = online();

        CambioDeReprogramacion cambio = turno.reprogramar(INICIO, 45, 8L, 2L);

        assertThat(cambio.nuevo()).containsOnly(
                Map.entry("fin", "2026-12-01T12:45:00Z"),
                Map.entry("profesional_id", "8"));
        assertThat(cambio.anterior()).containsOnly(
                Map.entry("fin", "2026-12-01T12:30:00Z"),
                Map.entry("profesional_id", "3"));
    }

    @Test
    void reprogramarSinCambiosDevuelveUnCambioVacio() {
        CambioDeReprogramacion cambio = online().reprogramar(INICIO, 30, 3L, 2L);

        assertThat(cambio.hayCambios()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ASISTIO", "AUSENTE", "AUSENTE_CON_AVISO"})
    void noSePuedeReprogramarUnTurnoCerrado(EstadoTurno estado) {
        Turno turno = online();
        turno.cambiarEstado(estado);

        assertThatThrownBy(() -> turno.reprogramar(Instant.parse("2026-12-02T15:30:00Z"), 30, 3L, 2L))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);
        assertThat(turno.getInicio()).isEqualTo(INICIO);
    }

    @Test
    void reprogramarConInicioConSegundosSeRechazaSinCambiarNada() {
        Turno turno = online();

        assertThatThrownBy(() -> turno.reprogramar(INICIO.plusSeconds(5), 30, 3L, 2L))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(turno.getInicio()).isEqualTo(INICIO);
    }
}

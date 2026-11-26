package ar.dentaflow.dominio.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ListaEsperaTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");

    @Test
    void creaEntradaActivaConRangoValidoYProfesionalOpcional() {
        ListaEspera entrada = ListaEspera.nueva(
                10L, 20L, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                LocalTime.of(9, 0), LocalTime.of(13, 0),
                1L, AHORA);

        assertThat(entrada.getEstado()).isEqualTo(EstadoListaEspera.ACTIVA);
        assertThat(entrada.getProfesionalId()).isNull();
        assertThat(entrada.getFechaDesde()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(entrada.getFechaHasta()).isEqualTo(LocalDate.of(2026, 12, 5));
    }

    @Test
    void rechazaFechaHastaAnteriorAFechaDesdeOInicioDeHorasInvalido() {
        assertThatThrownBy(() -> ListaEspera.nueva(
                10L, 20L, null,
                LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 1),
                null, null, 1L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThatThrownBy(() -> ListaEspera.nueva(
                10L, 20L, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                LocalTime.of(14, 0), LocalTime.of(10, 0), 1L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThatThrownBy(() -> ListaEspera.nueva(
                10L, 20L, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                LocalTime.of(9, 0), null, 1L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void transicionaDeActivaAOfrecidaYAtendidaOVencida() {
        ListaEspera entrada = ListaEspera.nueva(
                10L, 20L, 30L,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                LocalTime.of(9, 0), LocalTime.of(13, 0),
                1L, AHORA);

        Instant inicioHueco = Instant.parse("2026-12-02T13:00:00Z"); // 10:00 Buenos Aires
        Instant vence = AHORA.plusSeconds(14400);
        entrada.ofrecer(50L, 30L, inicioHueco, "tok-123", AHORA, vence);

        assertThat(entrada.getEstado()).isEqualTo(EstadoListaEspera.OFRECIDA);
        assertThat(entrada.getTokenAceptacion()).isEqualTo("tok-123");
        assertThat(entrada.estaVencida(AHORA.plusSeconds(60))).isFalse();
        assertThat(entrada.estaVencida(vence)).isTrue();

        entrada.marcarAtendida(99L);
        assertThat(entrada.getEstado()).isEqualTo(EstadoListaEspera.ATENDIDA);
        assertThat(entrada.getTurnoCreadoId()).isEqualTo(99L);
    }

    @Test
    void rechazaDarDeBajaSiNoEstaActiva() {
        ListaEspera entrada = ListaEspera.nueva(
                10L, 20L, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5),
                null, null, 1L, AHORA);
        entrada.ofrecer(50L, 30L, Instant.parse("2026-12-02T13:00:00Z"), "tok-123", AHORA, AHORA.plusSeconds(3600));

        assertThatThrownBy(entrada::darDeBaja)
                .isInstanceOf(ConflictoException.class);
    }
}

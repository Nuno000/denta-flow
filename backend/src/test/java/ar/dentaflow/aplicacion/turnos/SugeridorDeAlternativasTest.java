package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.agenda.ConsultaDeHuecos;
import ar.dentaflow.aplicacion.agenda.MotorDeDisponibilidad;
import ar.dentaflow.aplicacion.agenda.ReglasDeConsultaDeHuecos;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Spec carga-manual-turnos — "Alternativas sugeridas ante un conflicto" (design D3). */
@DisplayName("SugeridorDeAlternativas")
class SugeridorDeAlternativasTest {

    private static final long PRESTACION = 3L;
    private static final long PROFESIONAL = 1L;
    /** 2026-12-01T09:30-03:00. */
    private static final Instant PEDIDO = Instant.parse("2026-12-01T12:30:00Z");

    private final MotorDeDisponibilidad motor = mock(MotorDeDisponibilidad.class);

    private SugeridorDeAlternativas sugeridor(int diasMaximos) {
        return new SugeridorDeAlternativas(motor, new ReglasDeConsultaDeHuecos(15, diasMaximos));
    }

    private static Hueco hueco(String inicioUtc, long... sillones) {
        Instant inicio = Instant.parse(inicioUtc);
        List<CandidatoDeReserva> candidatos = Arrays.stream(sillones)
                .mapToObj(sillon -> new CandidatoDeReserva(PROFESIONAL, sillon)).toList();
        return new Hueco(inicio, inicio.plusSeconds(1800), candidatos);
    }

    @Test
    @DisplayName("ofrece los 3 huecos más cercanos al inicio pedido, ordenados por distancia")
    void ofreceLosTresMasCercanosOrdenadosPorDistancia() {
        when(motor.buscarHuecos(any())).thenReturn(List.of(
                hueco("2026-12-01T12:00:00Z", 1), // 09:00, 30 min antes
                hueco("2026-12-01T13:00:00Z", 1), // 10:00, 30 min después
                hueco("2026-12-01T13:15:00Z", 1), // 10:15, 45 min después
                hueco("2026-12-01T14:00:00Z", 1), // 11:00, 90 min después
                hueco("2026-12-01T11:00:00Z", 1))); // 08:00, 90 min antes

        List<AlternativaDeTurno> alternativas = sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 1L, PEDIDO);

        assertThat(alternativas).extracting(AlternativaDeTurno::inicio).containsExactly(
                Instant.parse("2026-12-01T12:00:00Z"),
                Instant.parse("2026-12-01T13:00:00Z"),
                Instant.parse("2026-12-01T13:15:00Z"));
    }

    @Test
    @DisplayName("a igual distancia gana el inicio más temprano")
    void aIgualDistanciaGanaElMasTemprano() {
        when(motor.buscarHuecos(any())).thenReturn(List.of(
                hueco("2026-12-01T13:00:00Z", 1), // +30
                hueco("2026-12-01T12:00:00Z", 1))); // -30

        List<AlternativaDeTurno> alternativas = sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 1L, PEDIDO);

        assertThat(alternativas).extracting(AlternativaDeTurno::inicio).containsExactly(
                Instant.parse("2026-12-01T12:00:00Z"), Instant.parse("2026-12-01T13:00:00Z"));
    }

    @Test
    @DisplayName("cada alternativa trae fin, profesional y el sillón pedido si está entre los candidatos")
    void prefiereElSillonPedido() {
        when(motor.buscarHuecos(any())).thenReturn(List.of(
                hueco("2026-12-01T13:00:00Z", 1, 2), // incluye el sillón 2
                hueco("2026-12-01T13:15:00Z", 4, 5))); // no lo incluye: el primero

        List<AlternativaDeTurno> alternativas = sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 2L, PEDIDO);

        assertThat(alternativas).containsExactly(
                new AlternativaDeTurno(Instant.parse("2026-12-01T13:00:00Z"), Instant.parse("2026-12-01T13:30:00Z"),
                        PROFESIONAL, 2L),
                new AlternativaDeTurno(Instant.parse("2026-12-01T13:15:00Z"), Instant.parse("2026-12-01T13:45:00Z"),
                        PROFESIONAL, 4L));
    }

    @Test
    @DisplayName("busca desde el día del consultorio hasta 6 días después, del mismo profesional y prestación")
    void ventanaDeSieteDias() {
        when(motor.buscarHuecos(any())).thenReturn(List.of());

        sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 1L, PEDIDO);

        ArgumentCaptor<ConsultaDeHuecos> consulta = ArgumentCaptor.forClass(ConsultaDeHuecos.class);
        verify(motor).buscarHuecos(consulta.capture());
        assertThat(consulta.getValue()).isEqualTo(new ConsultaDeHuecos(PRESTACION, PROFESIONAL,
                LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-07"), null));
    }

    @Test
    @DisplayName("el día se toma en el consultorio: 23:30-03:00 (ya es 2 de diciembre en UTC) busca desde el 1")
    void ventanaPorDiaLocal() {
        when(motor.buscarHuecos(any())).thenReturn(List.of());

        sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 1L, Instant.parse("2026-12-02T02:30:00Z"));

        ArgumentCaptor<ConsultaDeHuecos> consulta = ArgumentCaptor.forClass(ConsultaDeHuecos.class);
        verify(motor).buscarHuecos(consulta.capture());
        assertThat(consulta.getValue().desde()).isEqualTo(LocalDate.parse("2026-12-01"));
        assertThat(consulta.getValue().hasta()).isEqualTo(LocalDate.parse("2026-12-07"));
    }

    @Test
    @DisplayName("si dias-maximos es menor que 7, la ventana se acota a ese máximo")
    void ventanaAcotadaPorDiasMaximos() {
        when(motor.buscarHuecos(any())).thenReturn(List.of());

        sugeridor(3).sugerir(PRESTACION, PROFESIONAL, 1L, PEDIDO);

        ArgumentCaptor<ConsultaDeHuecos> consulta = ArgumentCaptor.forClass(ConsultaDeHuecos.class);
        verify(motor).buscarHuecos(consulta.capture());
        assertThat(consulta.getValue().hasta()).isEqualTo(LocalDate.parse("2026-12-03"));
    }

    @Test
    @DisplayName("sin huecos devuelve una lista vacía")
    void sinHuecosListaVacia() {
        when(motor.buscarHuecos(any())).thenReturn(List.of());

        assertThat(sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 1L, PEDIDO)).isEmpty();
    }

    @Test
    @DisplayName("si el motor falla devuelve una lista vacía y no tapa el conflicto original")
    void siElMotorFallaListaVacia() {
        when(motor.buscarHuecos(any())).thenThrow(new RecursoNoEncontradoException("No existe"));

        assertThat(sugeridor(14).sugerir(PRESTACION, PROFESIONAL, 1L, PEDIDO)).isEmpty();
    }
}

package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.RecursoOcupado;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.PessimisticLockingFailureException;

class ServicioDeReservaDeHuecosTest {

    private static final Instant INICIO = Instant.parse("2026-11-30T13:00:00Z");

    private final MotorDeDisponibilidad motor = mock(MotorDeDisponibilidad.class);
    private final ServicioDeTurnos turnos = mock(ServicioDeTurnos.class);
    private final ServicioDeReservaDeHuecos servicio = new ServicioDeReservaDeHuecos(motor, turnos);

    private static SolicitudDeReservaDeHueco solicitud(Long profesionalId) {
        return new SolicitudDeReservaDeHueco(10L, 1L, profesionalId, INICIO, OrigenTurno.ONLINE, Actor.paciente());
    }

    @Test
    void siPrimerCandidatoSolapaAgendaNoDisponibleOInterbloqueoPruebaElSiguienteYEnviaDatosCorrectos() {
        when(motor.candidatosPara(1L, null, INICIO, null)).thenReturn(List.of(
                new CandidatoDeReserva(1L, 100L),
                new CandidatoDeReserva(2L, 100L),
                new CandidatoDeReserva(3L, 200L),
                new CandidatoDeReserva(4L, 200L)));
        TurnoCreado esperado = new TurnoCreado(mock(Turno.class), "tok");

        when(turnos.crear(any()))
                .thenThrow(new TurnoSolapadoException(RecursoOcupado.PROFESIONAL))
                .thenThrow(new AgendaNoDisponibleException("bloqueado"))
                .thenThrow(new PessimisticLockingFailureException("deadlock"))
                .thenReturn(esperado);

        TurnoCreado resultado = servicio.reservar(solicitud(null));

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<SolicitudDeTurno> captor = ArgumentCaptor.forClass(SolicitudDeTurno.class);
        verify(turnos, times(4)).crear(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(s -> {
            assertThat(s.esSobreturno()).isFalse();
            assertThat(s.origen()).isEqualTo(OrigenTurno.ONLINE);
            assertThat(s.actor()).isEqualTo(Actor.paciente());
        });
        assertThat(captor.getAllValues().get(3).profesionalId()).isEqualTo(4L);
        assertThat(captor.getAllValues().get(3).sillonId()).isEqualTo(200L);
    }

    @Test
    void siTodosFallanONoHayCandidatosLanzaHuecoNoDisponibleException() {
        when(motor.candidatosPara(1L, 5L, INICIO, null)).thenReturn(List.of());
        assertThatThrownBy(() -> servicio.reservar(solicitud(5L)))
                .isInstanceOf(HuecoNoDisponibleException.class)
                .isInstanceOf(ConflictoException.class);

        when(motor.candidatosPara(1L, 5L, INICIO, null)).thenReturn(List.of(new CandidatoDeReserva(5L, 100L)));
        when(turnos.crear(any())).thenThrow(new TurnoSolapadoException(RecursoOcupado.SILLON));
        assertThatThrownBy(() -> servicio.reservar(solicitud(5L)))
                .isInstanceOf(HuecoNoDisponibleException.class);
    }

    @Test
    void erroresNoDisponibilidadSePropaganSinProbarMasCandidatos() {
        when(motor.candidatosPara(1L, null, INICIO, null)).thenReturn(List.of(
                new CandidatoDeReserva(1L, 100L),
                new CandidatoDeReserva(2L, 100L)));

        when(turnos.crear(any()))
                .thenThrow(new RecursoNoEncontradoException("sin paciente"))
                .thenThrow(new ValidacionDeDominioException("dato invalido"));
        assertThatThrownBy(() -> servicio.reservar(solicitud(null)))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(turnos, times(1)).crear(any());

        assertThatThrownBy(() -> servicio.reservar(solicitud(null)))
                .isInstanceOf(ValidacionDeDominioException.class);
        verify(turnos, times(2)).crear(any());
    }
}

package ar.dentaflow.aplicacion.reservas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.agenda.ServicioDeReservaDeHuecos;
import ar.dentaflow.aplicacion.agenda.SolicitudDeReservaDeHueco;
import ar.dentaflow.aplicacion.notificaciones.ServicioDeNotificaciones;
import ar.dentaflow.aplicacion.pacientes.DatosPaciente;
import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tarea 5.2 (spec "Reserva online de huecos", design D2):
 * Orquestación del caso de uso de reserva pública sin @Transactional.
 */
@ExtendWith(MockitoExtension.class)
class ServicioDeReservaOnlineTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-12-02T13:00:00Z");
    private static final Instant FIN = Instant.parse("2026-12-02T13:30:00Z");
    private static final String IP = "192.0.2.10";

    @Mock
    private LimitadorDeReservasPorOrigen limitador;
    @Mock
    private ServicioDePacientes pacientes;
    @Mock
    private ServicioDeReservaDeHuecos reservaDeHuecos;
    @Mock
    private ServicioDeNotificaciones notificaciones;
    @Mock
    private PrestacionRepositorio prestaciones;
    @Mock
    private ProfesionalRepositorio profesionales;

    private final Clock reloj = Clock.fixed(T0, ZoneOffset.UTC);
    private ServicioDeReservaOnline servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioDeReservaOnline(
                limitador, pacientes, reservaDeHuecos, notificaciones, prestaciones, profesionales, reloj);
    }

    private SolicitudDeReservaOnline solicitudValida(Boolean aceptaAviso) {
        return new SolicitudDeReservaOnline(
                10L, 20L, INICIO, "30111222", "Ana", "García", "0261 555-1234", "ana@example.com", aceptaAviso, IP);
    }

    private Turno mockTurno(long id, Long profesionalId) {
        Turno t = org.mockito.Mockito.mock(Turno.class);
        when(t.getId()).thenReturn(id);
        when(t.getPrestacionId()).thenReturn(10L);
        when(t.getProfesionalId()).thenReturn(profesionalId);
        when(t.getInicio()).thenReturn(INICIO);
        when(t.getFin()).thenReturn(FIN);
        when(t.getEstado()).thenReturn(EstadoTurno.PENDIENTE);
        return t;
    }

    @Test
    void elLimitadorCorrePrimeroYSiFallaNoTocaNadaMas() {
        doThrow(new DemasiadasReservasException(Duration.ofMinutes(5))).when(limitador).registrarPedido(IP);

        assertThatThrownBy(() -> servicio.reservar(solicitudValida(true)))
                .isInstanceOf(DemasiadasReservasException.class);

        verify(limitador).registrarPedido(IP);
        verifyNoInteractions(pacientes, reservaDeHuecos, notificaciones);
    }

    @Test
    void avisoNoAceptadoRechazaSinLlamarAPacientesNiReservasNiNotificaciones() {
        for (Boolean aviso : new Boolean[] {false, null}) {
            assertThatThrownBy(() -> servicio.reservar(solicitudValida(aviso)))
                    .isInstanceOfSatisfying(ValidacionDeDominioException.class,
                            e -> assertThat(e.getMessage()).isEqualTo(ServicioDePacientes.AVISO_NO_ACEPTADO));
        }

        verify(limitador, org.mockito.Mockito.times(2)).registrarPedido(IP);
        verifyNoInteractions(pacientes, reservaDeHuecos, notificaciones);
    }

    @Test
    void ordenEstrictoDePasosYNotificacionConTurnoId() {
        Paciente pacienteMock = Paciente.nuevo("30111222", "Ana", "García", "0261 555-1234", "ana@example.com", T0);
        ReflectionTestUtils.setField(pacienteMock, "id", 30L);
        when(pacientes.registrarDesdeReservaOnline(any())).thenReturn(pacienteMock);

        Turno turnoCreado = mockTurno(100L, 20L);
        when(reservaDeHuecos.reservar(any())).thenReturn(new TurnoCreado(turnoCreado, "token-123"));

        Prestacion prestacion = Prestacion.nueva("Limpieza", 30);
        Profesional profesional = Profesional.nuevo("Carlos", "López", "MP1234", "carlos@example.com");
        when(prestaciones.buscarPorId(10L)).thenReturn(Optional.of(prestacion));
        when(profesionales.buscarPorId(20L)).thenReturn(Optional.of(profesional));

        ReservaConfirmada confirmada = servicio.reservar(solicitudValida(true));

        InOrder inOrder = inOrder(limitador, pacientes, reservaDeHuecos, notificaciones);
        inOrder.verify(limitador).registrarPedido(IP);
        inOrder.verify(pacientes).registrarDesdeReservaOnline(new DatosPaciente(
                "30111222", "Ana", "García", "0261 555-1234", "ana@example.com", T0));
        inOrder.verify(reservaDeHuecos).reservar(new SolicitudDeReservaDeHueco(
                30L, 10L, 20L, INICIO, OrigenTurno.ONLINE, Actor.paciente()));
        inOrder.verify(notificaciones).enviarConfirmacion(100L);

        assertThat(confirmada.inicio()).isEqualTo(INICIO);
        assertThat(confirmada.fin()).isEqualTo(FIN);
        assertThat(confirmada.prestacion()).isEqualTo("Limpieza");
        assertThat(confirmada.profesional()).isEqualTo("Carlos López");
        assertThat(confirmada.estado()).isEqualTo(EstadoTurno.PENDIENTE);
    }

    @Test
    void huecoNoDisponibleSePropagaYNoSeEnviaNotificacion() {
        Paciente pacienteMock = Paciente.nuevo("30111222", "Ana", "García", "0261 555-1234", "ana@example.com", T0);
        ReflectionTestUtils.setField(pacienteMock, "id", 30L);
        when(pacientes.registrarDesdeReservaOnline(any())).thenReturn(pacienteMock);

        when(reservaDeHuecos.reservar(any()))
                .thenThrow(new HuecoNoDisponibleException("El horario elegido ya no está disponible; elegí otro"));

        assertThatThrownBy(() -> servicio.reservar(solicitudValida(true)))
                .isInstanceOf(HuecoNoDisponibleException.class);

        verify(notificaciones, never()).enviarConfirmacion(anyLong());
    }
}

package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.agenda.ConsultaDeHuecos;
import ar.dentaflow.aplicacion.agenda.MotorDeDisponibilidad;
import ar.dentaflow.aplicacion.configuracion.ParametrosDelConsultorio;
import ar.dentaflow.aplicacion.notificaciones.ResolucionDeEnlace;
import ar.dentaflow.aplicacion.notificaciones.ResumenTurnoPaciente;
import ar.dentaflow.aplicacion.notificaciones.ServicioDeEnlaces;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.notificaciones.SituacionDeEnlace;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioDeGestionTurnoPacienteTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T10:00:00Z");
    private static final String TOKEN = "token-valido-123";

    @Mock
    private ServicioDeEnlaces enlaces;

    @Mock
    private ServicioDeTurnos turnos;

    @Mock
    private ParametrosDelConsultorio parametros;

    @Mock
    private MotorDeDisponibilidad motor;

    @Mock
    private Turno turno;

    private Clock reloj;
    private ServicioDeGestionTurnoPaciente servicio;

    @BeforeEach
    void setUp() {
        reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
        servicio = new ServicioDeGestionTurnoPaciente(enlaces, turnos, parametros, motor, reloj);
    }

    // --- Bloque B: Consultar y Confirmar ---

    @Test
    void consultarPorTokenInexistenteOLanzaRecursoNoEncontrado() {
        when(enlaces.resolver("invalido")).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.INEXISTENTE, Optional.empty()));

        assertThatThrownBy(() -> servicio.consultarPorToken("invalido"))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessageContaining("no es válido");
    }

    @Test
    void consultarPorTokenCanceladoOVencidoDevuelvePuedeModificarFalse() {
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.CANCELADO, "Limpieza", "Dra. Laura Ejemplo");
        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.CANCELADO, Optional.of(resumen)));

        TurnoPublicoDetalle detalle = servicio.consultarPorToken(TOKEN);

        assertThat(detalle.situacion()).isEqualTo(SituacionDeEnlace.CANCELADO);
        assertThat(detalle.puedeModificar()).isFalse();
    }

    @Test
    void consultarTurnoVigenteDentroDeAnticipacionDevuelvePuedeModificarTrue() {
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        TurnoPublicoDetalle detalle = servicio.consultarPorToken(TOKEN);

        assertThat(detalle.situacion()).isEqualTo(SituacionDeEnlace.VIGENTE);
        assertThat(detalle.puedeModificar()).isTrue();
    }

    @Test
    void consultarTurnoVigenteFueraDeAnticipacionDevuelvePuedeModificarFalse() {
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(12)), AHORA.plus(Duration.ofHours(13)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        TurnoPublicoDetalle detalle = servicio.consultarPorToken(TOKEN);

        assertThat(detalle.situacion()).isEqualTo(SituacionDeEnlace.VIGENTE);
        assertThat(detalle.puedeModificar()).isFalse();
    }

    @Test
    void confirmarPendienteVigenteLlamaAServicioDeTurnosConActorPacienteYDevuelveConfirmado() {
        ResumenTurnoPaciente resumenPendiente = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        ResumenTurnoPaciente resumenConfirmado = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.CONFIRMADO, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumenPendiente)))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumenConfirmado)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getId()).thenReturn(10L);
        when(turno.getEstado()).thenReturn(EstadoTurno.PENDIENTE);
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        TurnoPublicoDetalle resultado = servicio.confirmar(TOKEN);

        verify(turnos).confirmar(10L, Actor.paciente());
        assertThat(resultado.estado()).isEqualTo("confirmado");
    }

    @Test
    void confirmarTurnoYaConfirmadoEsIdempotenteYNoLlamaAServicioDeTurnos() {
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.CONFIRMADO, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getEstado()).thenReturn(EstadoTurno.CONFIRMADO);
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        TurnoPublicoDetalle resultado = servicio.confirmar(TOKEN);

        verify(turnos, never()).confirmar(any(Long.class), any(Actor.class));
        assertThat(resultado.estado()).isEqualTo("confirmado");
    }

    @Test
    void confirmarCanceladoOVencidoRechazaConConflictoException() {
        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.CANCELADO, Optional.empty()));

        assertThatThrownBy(() -> servicio.confirmar(TOKEN))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("cancelado");

        when(enlaces.resolver("vencido")).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VENCIDO, Optional.empty()));
        assertThatThrownBy(() -> servicio.confirmar("vencido"))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("vencido");
    }

    // --- Bloque C: Cancelación ---

    @Test
    void cancelarTurnoDentroDeAnticipacionLlamaAServicioDeTurnosConActorPaciente() {
        ResumenTurnoPaciente resumenPendiente = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        ResumenTurnoPaciente resumenCancelado = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.CANCELADO, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumenPendiente)))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.CANCELADO, Optional.of(resumenCancelado)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getId()).thenReturn(10L);
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofHours(48)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        TurnoPublicoDetalle resultado = servicio.cancelar(TOKEN);

        verify(turnos).cancelar(10L, Actor.paciente());
        assertThat(resultado.estado()).isEqualTo("cancelado");
    }

    @Test
    void cancelarFueraDeTerminoLanzaCancelacionFueraDeTerminoExceptionConMensajeDeContacto() {
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(12)), AHORA.plus(Duration.ofHours(13)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofHours(12)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));
        when(parametros.contactoConsultorio()).thenReturn("0261 555-1234");

        assertThatThrownBy(() -> servicio.cancelar(TOKEN))
                .isInstanceOf(CancelacionFueraDeTerminoException.class)
                .hasMessageContaining("vencido")
                .hasMessageContaining("consultorio")
                .hasMessageContaining("0261 555-1234");

        verify(turnos, never()).cancelar(any(Long.class), any(Actor.class));
    }

    @Test
    void cancelarConAnticipacionCeroPermiteCancelarHastaElMismoInicio() {
        ResumenTurnoPaciente resumenPendiente = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofMinutes(15)), AHORA.plus(Duration.ofMinutes(45)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        ResumenTurnoPaciente resumenCancelado = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofMinutes(15)), AHORA.plus(Duration.ofMinutes(45)),
                EstadoTurno.CANCELADO, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumenPendiente)))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.CANCELADO, Optional.of(resumenCancelado)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getId()).thenReturn(10L);
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofMinutes(15)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ZERO);

        TurnoPublicoDetalle resultado = servicio.cancelar(TOKEN);

        verify(turnos).cancelar(10L, Actor.paciente());
        assertThat(resultado.estado()).isEqualTo("cancelado");
    }

    // --- Bloque D: Reprogramación ---

    @Test
    void consultarHuecosParaReprogramarLlamaAMotorConTurnoAExcluir() {
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getId()).thenReturn(10L);
        when(turno.getPrestacionId()).thenReturn(1L);
        when(turno.getProfesionalId()).thenReturn(2L);
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofHours(48)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        LocalDate desde = LocalDate.of(2026, 12, 5);
        LocalDate hasta = LocalDate.of(2026, 12, 10);
        List<Hueco> huecosEsperados = List.of(new Hueco(
                AHORA.plus(Duration.ofDays(4)),
                AHORA.plus(Duration.ofDays(4)).plusSeconds(1800),
                List.of(new CandidatoDeReserva(2L, 5L))));
        when(motor.buscarHuecos(new ConsultaDeHuecos(1L, 2L, desde, hasta, 10L))).thenReturn(huecosEsperados);

        List<Hueco> resultado = servicio.consultarHuecosParaReprogramar(TOKEN, desde, hasta);

        assertThat(resultado).isEqualTo(huecosEsperados);
    }

    @Test
    void reprogramarDentroDeVentanaBuscaCandidatosConTurnoAExcluirYDelegaEnServicioDeTurnos() {
        Instant nuevoInicio = AHORA.plus(Duration.ofDays(3));
        ResumenTurnoPaciente resumenViejo = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");
        ResumenTurnoPaciente resumenNuevo = new ResumenTurnoPaciente(
                nuevoInicio, nuevoInicio.plusSeconds(1800),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumenViejo)))
                .thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumenNuevo)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getId()).thenReturn(10L);
        when(turno.getPrestacionId()).thenReturn(1L);
        when(turno.getProfesionalId()).thenReturn(2L);
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofHours(48)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        CandidatoDeReserva candidato = new CandidatoDeReserva(2L, 5L);
        when(motor.candidatosPara(1L, 2L, nuevoInicio, 10L)).thenReturn(List.of(candidato));

        TurnoPublicoDetalle resultado = servicio.reprogramar(TOKEN, nuevoInicio);

        verify(turnos).reprogramar(10L, new SolicitudDeReprogramacion(nuevoInicio, 2L, 5L), Actor.paciente());
        assertThat(resultado.inicio()).isEqualTo(nuevoInicio);
    }

    @Test
    void reprogramarFueraDeVentanaLanzaCancelacionFueraDeTerminoException() {
        Instant nuevoInicio = AHORA.plus(Duration.ofDays(3));
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(12)), AHORA.plus(Duration.ofHours(13)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofHours(12)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));
        when(parametros.contactoConsultorio()).thenReturn("0261 555-1234");

        assertThatThrownBy(() -> servicio.reprogramar(TOKEN, nuevoInicio))
                .isInstanceOf(CancelacionFueraDeTerminoException.class)
                .hasMessageContaining("vencido");

        verify(turnos, never()).reprogramar(any(Long.class), any(), any());
    }

    @Test
    void reprogramarSinCandidatosDisponiblesLanzaHuecoNoDisponibleException() {
        Instant nuevoInicio = AHORA.plus(Duration.ofDays(3));
        ResumenTurnoPaciente resumen = new ResumenTurnoPaciente(
                AHORA.plus(Duration.ofHours(48)), AHORA.plus(Duration.ofHours(49)),
                EstadoTurno.PENDIENTE, "Limpieza", "Dra. Laura Ejemplo");

        when(enlaces.resolver(TOKEN)).thenReturn(new ResolucionDeEnlace(SituacionDeEnlace.VIGENTE, Optional.of(resumen)));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));
        when(turno.getId()).thenReturn(10L);
        when(turno.getPrestacionId()).thenReturn(1L);
        when(turno.getProfesionalId()).thenReturn(2L);
        when(turno.getInicio()).thenReturn(AHORA.plus(Duration.ofHours(48)));
        when(parametros.anticipacionMinima()).thenReturn(Duration.ofHours(24));

        when(motor.candidatosPara(1L, 2L, nuevoInicio, 10L)).thenReturn(List.of());

        assertThatThrownBy(() -> servicio.reprogramar(TOKEN, nuevoInicio))
                .isInstanceOf(HuecoNoDisponibleException.class)
                .hasMessageContaining("no está disponible");

        verify(turnos, never()).reprogramar(any(Long.class), any(), any());
    }
}

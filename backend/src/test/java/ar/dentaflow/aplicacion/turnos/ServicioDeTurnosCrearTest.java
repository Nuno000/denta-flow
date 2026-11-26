package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.RecursoOcupado;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoPropuesto;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Spec turnos — "Recursos válidos", "Inicio no anterior", "Origen y actor", "Sin solapamiento por profesional ni
 * por sillón", "Sobreturno explícito", "Bloqueo en orden fijo", "Cambios auditados" y "Token" (design D5, D6).
 */
class ServicioDeTurnosCrearTest extends ServicioDeTurnosUnitarioBase {

    private SolicitudDeTurno online() {
        return new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID, PRESTACION_ID, INICIO,
                OrigenTurno.ONLINE, false, Actor.paciente());
    }

    private SolicitudDeTurno recepcion(boolean sobreturno) {
        return new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID, PRESTACION_ID, INICIO,
                OrigenTurno.RECEPCION, sobreturno, Actor.usuario(USUARIO_ID));
    }

    private void assertSinEscrituraNiAuditoria() {
        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }

    // --- Orden y resultado ---

    @Test
    void bloqueaProfesionalLuegoSillonVerificaGuardaYAudita() {
        TurnoCreado creado = servicio.crear(online());

        InOrder orden = inOrder(cerrojo, restriccion, turnos, auditoria);
        orden.verify(cerrojo).bloquearProfesional(PROFESIONAL_ID);
        orden.verify(cerrojo).bloquearSillon(SILLON_ID);
        orden.verify(restriccion).verificar(new TurnoPropuesto(PROFESIONAL_ID, SILLON_ID, PRESTACION_ID, INICIO,
                Instant.parse("2026-12-01T12:30:00Z"), false));
        orden.verify(turnos).haySolapamientoDeProfesional(PROFESIONAL_ID, INICIO,
                Instant.parse("2026-12-01T12:30:00Z"), null);
        orden.verify(turnos).haySolapamientoDeSillon(SILLON_ID, INICIO, Instant.parse("2026-12-01T12:30:00Z"), null);
        orden.verify(turnos).guardar(any(Turno.class));
        orden.verify(auditoria).registrarCambio(eq(TURNO_ID), eq(AccionDeTurno.CREACION), eq(Actor.paciente()),
                isNull(), eq(creado.turno().valoresDeAlta()));
        assertThat(creado.turno().getId()).isEqualTo(TURNO_ID);
        assertThat(creado.turno().getEstado()).isEqualTo(EstadoTurno.PENDIENTE);
        assertThat(creado.tokenAcceso()).isEqualTo(TOKEN);
    }

    @Test
    void elTurnoNaceConLosDatosDeLaSolicitudYElFinDeLaPrestacion() {
        TurnoCreado creado = servicio.crear(recepcion(false));

        Turno turno = creado.turno();
        assertThat(turno.getPacienteId()).isEqualTo(PACIENTE_ID);
        assertThat(turno.getOrigen()).isEqualTo(OrigenTurno.RECEPCION);
        assertThat(turno.getCreadoPorUsuarioId()).isEqualTo(USUARIO_ID);
        assertThat(turno.getCreadoEn()).isEqualTo(AHORA);
        assertThat(turno.getFin()).isEqualTo(INICIO.plusSeconds(30 * 60));
        verify(auditoria).registrarCambio(eq(TURNO_ID), eq(AccionDeTurno.CREACION),
                eq(Actor.usuario(USUARIO_ID)), isNull(), any());
    }

    @Test
    void laAuditoriaDeAltaNoLlevaElToken() {
        servicio.crear(online());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> nuevo = ArgumentCaptor.forClass(Map.class);
        verify(auditoria).registrarCambio(anyLong(), any(), any(), any(), nuevo.capture());
        assertThat(nuevo.getValue()).containsKeys("paciente_id", "es_sobreturno").doesNotContainValue(TOKEN);
    }

    // --- Rechazos sin guardar ni auditar ---

    @Test
    void unPacienteInexistenteSeRechaza() {
        when(pacientes.buscarPorId(PACIENTE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(RecursoNoEncontradoException.class);

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unProfesionalInexistenteSeRechaza() {
        when(profesionales.buscarPorId(PROFESIONAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(RecursoNoEncontradoException.class);

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unaPrestacionInexistenteSeRechaza() {
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(RecursoNoEncontradoException.class);

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unSillonInexistenteSeRechaza() {
        when(sillones.buscarPorId(SILLON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(RecursoNoEncontradoException.class);

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void recursosInactivosSeRechazan() {
        prestacion.desactivar();
        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("prestación");
        prestacion.activar();

        profesional.desactivar();
        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("profesional");
        profesional.activar();

        sillon.desactivar();
        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("sillón");

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unProfesionalQueNoRealizaLaPrestacionSeRechaza() {
        profesional.deshabilitarPrestacion(PRESTACION_ID);

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("no realiza");

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unInicioAnteriorAlRelojSeRechazaYUnoIgualSeAcepta() {
        SolicitudDeTurno pasado = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID, PRESTACION_ID,
                AHORA.minusSeconds(60), OrigenTurno.ONLINE, false, Actor.paciente());
        assertThatThrownBy(() -> servicio.crear(pasado)).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("anterior");
        assertSinEscrituraNiAuditoria();

        SolicitudDeTurno ahora = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID, PRESTACION_ID, AHORA,
                OrigenTurno.ONLINE, false, Actor.paciente());
        assertThat(servicio.crear(ahora).turno().getInicio()).isEqualTo(AHORA);
    }

    @Test
    void origenYActorTienenQueSerCoherentes() {
        SolicitudDeTurno onlineConUsuario = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID,
                PRESTACION_ID, INICIO, OrigenTurno.ONLINE, false, Actor.usuario(USUARIO_ID));
        SolicitudDeTurno recepcionConPaciente = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID,
                PRESTACION_ID, INICIO, OrigenTurno.RECEPCION, false, Actor.paciente());
        SolicitudDeTurno recepcionConSistema = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID,
                PRESTACION_ID, INICIO, OrigenTurno.RECEPCION, false, Actor.sistema());

        assertThatThrownBy(() -> servicio.crear(onlineConUsuario)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.crear(recepcionConPaciente))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.crear(recepcionConSistema))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unSobreturnoOnlineSeRechaza() {
        SolicitudDeTurno sobreturnoOnline = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID,
                PRESTACION_ID, INICIO, OrigenTurno.ONLINE, true, Actor.paciente());

        assertThatThrownBy(() -> servicio.crear(sobreturnoOnline)).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("sobreturno");

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void unInicioConSegundosSeRechaza() {
        SolicitudDeTurno conSegundos = new SolicitudDeTurno(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID, PRESTACION_ID,
                INICIO.plusSeconds(30), OrigenTurno.ONLINE, false, Actor.paciente());

        assertThatThrownBy(() -> servicio.crear(conSegundos)).isInstanceOf(ValidacionDeDominioException.class);

        assertSinEscrituraNiAuditoria();
    }

    // --- Solapamiento ---

    @Test
    void elSolapamientoDelProfesionalSeRechazaNombrandoAlProfesional() {
        when(turnos.haySolapamientoDeProfesional(eq(PROFESIONAL_ID), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOfSatisfying(TurnoSolapadoException.class,
                e -> assertThat(e.recurso()).isEqualTo(RecursoOcupado.PROFESIONAL));

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void elSolapamientoDelSillonSeRechazaNombrandoAlSillon() {
        when(turnos.haySolapamientoDeSillon(eq(SILLON_ID), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOfSatisfying(TurnoSolapadoException.class,
                e -> assertThat(e.recurso()).isEqualTo(RecursoOcupado.SILLON));

        assertSinEscrituraNiAuditoria();
    }

    @Test
    void siUnaRestriccionDeAgendaRechazaNoSeVerificaNiSeGuarda() {
        doThrow(new ValidacionDeDominioException("Fuera del horario de atención")).when(restriccion).verificar(any());

        assertThatThrownBy(() -> servicio.crear(online())).isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("horario");

        verify(turnos, never()).haySolapamientoDeProfesional(any(), any(), any(), any());
        verify(turnos, never()).haySolapamientoDeSillon(any(), any(), any(), any());
        assertSinEscrituraNiAuditoria();
    }

    // --- Sobreturno ---

    @Test
    void elSobreturnoDeRecepcionTomaLosDosBloqueosYNoConsultaSolapamiento() {
        TurnoCreado creado = servicio.crear(recepcion(true));

        InOrder orden = inOrder(cerrojo, restriccion, turnos, auditoria);
        orden.verify(cerrojo).bloquearProfesional(PROFESIONAL_ID);
        orden.verify(cerrojo).bloquearSillon(SILLON_ID);
        orden.verify(restriccion).verificar(any(TurnoPropuesto.class));
        orden.verify(turnos).guardar(any(Turno.class));
        orden.verify(auditoria).registrarCambio(eq(TURNO_ID), eq(AccionDeTurno.CREACION), any(), isNull(), any());
        verify(turnos, never()).haySolapamientoDeProfesional(any(), any(), any(), any());
        verify(turnos, never()).haySolapamientoDeSillon(any(), any(), any(), any());
        assertThat(creado.turno().isEsSobreturno()).isTrue();
        assertThat(creado.turno().valoresDeAlta()).containsEntry("es_sobreturno", "true");
    }
}

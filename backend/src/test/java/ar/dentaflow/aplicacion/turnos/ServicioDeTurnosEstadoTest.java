package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.TransicionDeEstadoInvalidaException;
import ar.dentaflow.dominio.turnos.Turno;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Spec turnos — "Transiciones de estado explícitas", "Repetir el estado actual es idempotente", "Cambios auditados". */
class ServicioDeTurnosEstadoTest extends ServicioDeTurnosUnitarioBase {

    private static final Actor RECEPCION = Actor.usuario(USUARIO_ID);

    private Turno existente(EstadoTurno estado) {
        Turno turno = turnoExistente(estado);
        when(turnos.buscarPorIdParaActualizar(TURNO_ID)).thenReturn(Optional.of(turno));
        return turno;
    }

    private void assertAuditado(AccionDeTurno accion, Actor actor, String anterior, String nuevo) {
        verify(auditoria).registrarCambio(TURNO_ID, accion, actor, Map.of("estado", anterior),
                Map.of("estado", nuevo));
    }

    @Test
    void confirmarAuditaConfirmacionConElActorRecibido() {
        Turno turno = existente(EstadoTurno.PENDIENTE);

        Turno resultado = servicio.confirmar(TURNO_ID, RECEPCION);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(turno.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
        verify(turnos).guardar(turno);
        assertAuditado(AccionDeTurno.CONFIRMACION, RECEPCION, "pendiente", "confirmado");
    }

    @Test
    void cancelarAuditaCancelacionConElActorRecibido() {
        existente(EstadoTurno.CONFIRMADO);

        Turno resultado = servicio.cancelar(TURNO_ID, Actor.paciente());

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
        assertAuditado(AccionDeTurno.CANCELACION, Actor.paciente(), "confirmado", "cancelado");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"ASISTIO", "AUSENTE", "AUSENTE_CON_AVISO"})
    void cambiarEstadoDeAsistenciaAuditaCambioDeEstado(EstadoTurno destino) {
        existente(EstadoTurno.CONFIRMADO);

        Turno resultado = servicio.cambiarEstado(TURNO_ID, destino, RECEPCION);

        assertThat(resultado.getEstado()).isEqualTo(destino);
        assertAuditado(AccionDeTurno.CAMBIO_ESTADO, RECEPCION, "confirmado", destino.codigo());
    }

    @Test
    void cambiarEstadoACanceladoOConfirmadoUsaLaAccionEspecifica() {
        existente(EstadoTurno.PENDIENTE);
        servicio.cambiarEstado(TURNO_ID, EstadoTurno.CONFIRMADO, RECEPCION);
        assertAuditado(AccionDeTurno.CONFIRMACION, RECEPCION, "pendiente", "confirmado");

        servicio.cambiarEstado(TURNO_ID, EstadoTurno.CANCELADO, RECEPCION);
        assertAuditado(AccionDeTurno.CANCELACION, RECEPCION, "confirmado", "cancelado");
    }

    @Test
    void usaElBloqueoDeLaFilaDelTurnoYNuncaElCerrojoDeRecursos() {
        existente(EstadoTurno.PENDIENTE);

        servicio.confirmar(TURNO_ID, RECEPCION);

        verify(turnos).buscarPorIdParaActualizar(TURNO_ID);
        verify(turnos, never()).buscarPorId(any());
        verifyNoInteractions(cerrojo);
    }

    @Test
    void repetirElEstadoNoGuardaNiAudita() {
        existente(EstadoTurno.CONFIRMADO);

        Turno resultado = servicio.confirmar(TURNO_ID, RECEPCION);

        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void repetirLaCancelacionNoGuardaNiAudita() {
        existente(EstadoTurno.CANCELADO);

        servicio.cancelar(TURNO_ID, RECEPCION);

        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void unaTransicionInvalidaSeRechazaSinAuditar() {
        Turno turno = existente(EstadoTurno.CANCELADO);

        assertThatThrownBy(() -> servicio.confirmar(TURNO_ID, RECEPCION))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);

        assertThat(turno.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void volverAPendienteDesdeConfirmadoSeRechaza() {
        existente(EstadoTurno.CONFIRMADO);

        assertThatThrownBy(() -> servicio.cambiarEstado(TURNO_ID, EstadoTurno.PENDIENTE, RECEPCION))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);

        verifyNoInteractions(auditoria);
    }

    @Test
    void unTurnoInexistenteSeRechaza() {
        when(turnos.buscarPorIdParaActualizar(TURNO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cancelar(TURNO_ID, RECEPCION))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(auditoria);
    }

    @Test
    void buscarPorIdYPorTokenDelegan() {
        Turno turno = turnoExistente(EstadoTurno.PENDIENTE);
        when(turnos.buscarPorId(TURNO_ID)).thenReturn(Optional.of(turno));
        when(turnos.buscarPorToken(TOKEN)).thenReturn(Optional.of(turno));

        assertThat(servicio.buscarPorId(TURNO_ID)).containsSame(turno);
        assertThat(servicio.buscarPorToken(TOKEN)).containsSame(turno);
        assertThat(servicio.buscarPorToken("otro")).isEmpty();
    }
}

package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.RecursoOcupado;
import ar.dentaflow.dominio.turnos.TransicionDeEstadoInvalidaException;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoPropuesto;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

/** Spec turnos — "Reprogramación verificada" y "Reprogramación auditada en UTC" (design D5, D6). */
class ServicioDeTurnosReprogramarTest extends ServicioDeTurnosUnitarioBase {

    private static final Instant NUEVO_INICIO = Instant.parse("2026-12-02T15:30:00Z");
    private static final Instant NUEVO_FIN = Instant.parse("2026-12-02T16:00:00Z");
    private static final long OTRO_PROFESIONAL_ID = 8L;
    private static final long OTRO_SILLON_ID = 4L;
    private static final Actor ACTOR = Actor.usuario(USUARIO_ID);

    private Turno existente(EstadoTurno estado) {
        Turno turno = turnoExistente(estado);
        when(turnos.buscarPorIdParaActualizar(TURNO_ID)).thenReturn(Optional.of(turno));
        return turno;
    }

    private SolicitudDeReprogramacion aOtroSillon() {
        return new SolicitudDeReprogramacion(NUEVO_INICIO, PROFESIONAL_ID, OTRO_SILLON_ID);
    }

    private void habilitarDestino() {
        Profesional otro = AyudanteDeIds.conId(Profesional.nuevo("Otro", "Profesional", null, "otro@example.com"),
                OTRO_PROFESIONAL_ID);
        otro.habilitarPrestacion(prestacion);
        when(profesionales.buscarPorId(OTRO_PROFESIONAL_ID)).thenReturn(Optional.of(otro));
        when(sillones.buscarPorId(OTRO_SILLON_ID))
                .thenReturn(Optional.of(AyudanteDeIds.conId(Sillon.nuevo("Sillón 4"), OTRO_SILLON_ID)));
    }

    @Test
    void bloqueaDestinoLuegoElTurnoVerificaExcluyendoAlPropioGuardaYAuditaSoloLoQueCambio() {
        habilitarDestino();
        Turno turno = existente(EstadoTurno.CONFIRMADO);

        Turno resultado = servicio.reprogramar(TURNO_ID, aOtroSillon(), ACTOR);

        InOrder orden = inOrder(cerrojo, turnos, restriccion, auditoria);
        orden.verify(cerrojo).bloquearProfesional(PROFESIONAL_ID);
        orden.verify(cerrojo).bloquearSillon(OTRO_SILLON_ID);
        orden.verify(turnos).buscarPorIdParaActualizar(TURNO_ID);
        orden.verify(restriccion).verificar(
                new TurnoPropuesto(PROFESIONAL_ID, OTRO_SILLON_ID, PRESTACION_ID, NUEVO_INICIO, NUEVO_FIN, false));
        orden.verify(turnos).haySolapamientoDeProfesional(PROFESIONAL_ID, NUEVO_INICIO, NUEVO_FIN, TURNO_ID);
        orden.verify(turnos).haySolapamientoDeSillon(OTRO_SILLON_ID, NUEVO_INICIO, NUEVO_FIN, TURNO_ID);
        orden.verify(turnos).guardar(turno);
        orden.verify(auditoria).registrarCambio(TURNO_ID, AccionDeTurno.REPROGRAMACION, ACTOR,
                Map.of("inicio", "2026-12-01T12:00:00Z", "fin", "2026-12-01T12:30:00Z", "sillon_id", "2"),
                Map.of("inicio", "2026-12-02T15:30:00Z", "fin", "2026-12-02T16:00:00Z", "sillon_id", "4"));
        assertThat(resultado.getInicio()).isEqualTo(NUEVO_INICIO);
        assertThat(resultado.getSillonId()).isEqualTo(OTRO_SILLON_ID);
        assertThat(resultado.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
    }

    @Test
    void cambiarDeProfesionalBloqueaAlProfesionalDestino() {
        habilitarDestino();
        existente(EstadoTurno.PENDIENTE);

        servicio.reprogramar(TURNO_ID, new SolicitudDeReprogramacion(NUEVO_INICIO, OTRO_PROFESIONAL_ID, SILLON_ID),
                ACTOR);

        verify(cerrojo).bloquearProfesional(OTRO_PROFESIONAL_ID);
        verify(auditoria).registrarCambio(eq(TURNO_ID), eq(AccionDeTurno.REPROGRAMACION), eq(ACTOR),
                eq(Map.of("inicio", "2026-12-01T12:00:00Z", "fin", "2026-12-01T12:30:00Z", "profesional_id", "3")),
                eq(Map.of("inicio", "2026-12-02T15:30:00Z", "fin", "2026-12-02T16:00:00Z", "profesional_id", "8")));
    }

    @Test
    void unDestinoOcupadoPorElProfesionalSeRechazaSinGuardarNiAuditar() {
        habilitarDestino();
        Turno turno = existente(EstadoTurno.PENDIENTE);
        when(turnos.haySolapamientoDeProfesional(PROFESIONAL_ID, NUEVO_INICIO, NUEVO_FIN, TURNO_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> servicio.reprogramar(TURNO_ID, aOtroSillon(), ACTOR))
                .isInstanceOfSatisfying(TurnoSolapadoException.class,
                        e -> assertThat(e.recurso()).isEqualTo(RecursoOcupado.PROFESIONAL));

        assertThat(turno.getInicio()).isEqualTo(INICIO);
        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void unDestinoOcupadoPorElSillonSeRechaza() {
        habilitarDestino();
        existente(EstadoTurno.PENDIENTE);
        when(turnos.haySolapamientoDeSillon(OTRO_SILLON_ID, NUEVO_INICIO, NUEVO_FIN, TURNO_ID)).thenReturn(true);

        assertThatThrownBy(() -> servicio.reprogramar(TURNO_ID, aOtroSillon(), ACTOR))
                .isInstanceOfSatisfying(TurnoSolapadoException.class,
                        e -> assertThat(e.recurso()).isEqualTo(RecursoOcupado.SILLON));

        verifyNoInteractions(auditoria);
    }

    @Test
    void unTurnoCanceladoNoSeReprograma() {
        habilitarDestino();
        existente(EstadoTurno.CANCELADO);

        assertThatThrownBy(() -> servicio.reprogramar(TURNO_ID, aOtroSillon(), ACTOR))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);

        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }

    @Test
    void unInicioEnElPasadoSeRechaza() {
        habilitarDestino();
        existente(EstadoTurno.PENDIENTE);
        SolicitudDeReprogramacion alPasado = new SolicitudDeReprogramacion(AHORA.minusSeconds(60), PROFESIONAL_ID,
                OTRO_SILLON_ID);

        assertThatThrownBy(() -> servicio.reprogramar(TURNO_ID, alPasado, ACTOR))
                .isInstanceOf(ValidacionDeDominioException.class);

        verifyNoInteractions(auditoria);
    }

    @Test
    void unDestinoInactivoOQueNoRealizaLaPrestacionSeRechaza() {
        existente(EstadoTurno.PENDIENTE);
        Profesional sinPrestacion = AyudanteDeIds.conId(
                Profesional.nuevo("Sin", "Prestacion", null, "sin@example.com"), OTRO_PROFESIONAL_ID);
        when(profesionales.buscarPorId(OTRO_PROFESIONAL_ID)).thenReturn(Optional.of(sinPrestacion));
        when(sillones.buscarPorId(OTRO_SILLON_ID))
                .thenReturn(Optional.of(AyudanteDeIds.conId(Sillon.nuevo("Sillón 4"), OTRO_SILLON_ID)));

        assertThatThrownBy(() -> servicio.reprogramar(TURNO_ID,
                new SolicitudDeReprogramacion(NUEVO_INICIO, OTRO_PROFESIONAL_ID, OTRO_SILLON_ID), ACTOR))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("no realiza");

        verifyNoInteractions(auditoria);
    }

    @Test
    void reprogramarAlMismoLugarNoGuardaNiAudita() {
        existente(EstadoTurno.PENDIENTE);

        servicio.reprogramar(TURNO_ID, new SolicitudDeReprogramacion(INICIO, PROFESIONAL_ID, SILLON_ID), ACTOR);

        verify(turnos, never()).guardar(any());
        verifyNoInteractions(auditoria);
    }
}

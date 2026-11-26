package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Instant;

/**
 * Situación del turno al que lleva un enlace del paciente (spec enlaces-paciente, design D6). El enlace vence con
 * el turno: no hay fecha de vencimiento propia del token. La anticipación mínima para cancelar o reprogramar
 * (RN-CA-01) la verifica C-17 sobre un turno {@link #VIGENTE}.
 */
public enum SituacionDeEnlace {
    /** Turno {@code pendiente} o {@code confirmado} que todavía no empezó. */
    VIGENTE,
    /** Turno cancelado. */
    CANCELADO,
    /** El turno ya empezó o ya tiene la asistencia registrada. */
    VENCIDO,
    /** Token mal formado o sin turno: no se distingue cuál de los dos. */
    INEXISTENTE;

    /** Situación de un turno existente en el instante {@code ahora} (UTC). */
    public static SituacionDeEnlace de(Turno turno, Instant ahora) {
        EstadoTurno estado = turno.getEstado();
        if (estado == EstadoTurno.CANCELADO) {
            return CANCELADO;
        }
        boolean activo = estado == EstadoTurno.PENDIENTE || estado == EstadoTurno.CONFIRMADO;
        if (!activo || !ahora.isBefore(turno.getInicio())) {
            return VENCIDO;
        }
        return VIGENTE;
    }
}

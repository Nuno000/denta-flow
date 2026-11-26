package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import java.time.Instant;

/**
 * Lo que el paciente ve de su turno al abrir un enlace (spec "La resolución no expone ni modifica el turno"). Sin
 * datos del paciente ni ids internos. {@code profesional} es nombre y apellido.
 */
public record ResumenTurnoPaciente(Instant inicio, Instant fin, EstadoTurno estado, String prestacion,
        String profesional) {
}

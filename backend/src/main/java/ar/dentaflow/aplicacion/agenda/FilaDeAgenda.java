package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import java.time.Instant;

/**
 * Un turno tal como lo muestra la agenda: proyección de solo lectura, sin DNI, teléfono, email ni token del
 * paciente (Ley 25.326, spec "Datos de cada turno en la agenda"). Un paciente anonimizado (C-15) llega con los
 * valores sustitutos de su nombre y apellido.
 */
public record FilaDeAgenda(
        Long turnoId,
        Instant inicio,
        Instant fin,
        EstadoTurno estado,
        OrigenTurno origen,
        boolean esSobreturno,
        String pacienteNombre,
        String pacienteApellido,
        Long prestacionId,
        String prestacionNombre,
        Long profesionalId,
        String profesionalNombre,
        String profesionalApellido,
        Long sillonId,
        String sillonNombre) {
}

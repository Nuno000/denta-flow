package ar.dentaflow.presentacion.agenda;

import ar.dentaflow.aplicacion.agenda.FilaDeAgenda;
import java.time.Instant;

/**
 * Un turno de {@code GET /api/agenda/turnos} (C-11, design D1). Proyección mínima: el paciente solo con nombre y
 * apellido (Ley 25.326), sin DNI, teléfono, email ni token. Instantes en UTC (ISO-8601); estado y origen como código.
 */
public record TurnoDeAgendaRespuesta(
        Long id,
        Instant inicio,
        Instant fin,
        String estado,
        String origen,
        boolean esSobreturno,
        PacienteDeAgenda paciente,
        Referencia prestacion,
        ProfesionalDeAgenda profesional,
        Referencia sillon) {

    public record PacienteDeAgenda(String nombre, String apellido) {
    }

    public record ProfesionalDeAgenda(Long id, String nombre, String apellido) {
    }

    public record Referencia(Long id, String nombre) {
    }

    static TurnoDeAgendaRespuesta de(FilaDeAgenda fila) {
        return new TurnoDeAgendaRespuesta(
                fila.turnoId(),
                fila.inicio(),
                fila.fin(),
                fila.estado().codigo(),
                fila.origen().codigo(),
                fila.esSobreturno(),
                new PacienteDeAgenda(fila.pacienteNombre(), fila.pacienteApellido()),
                new Referencia(fila.prestacionId(), fila.prestacionNombre()),
                new ProfesionalDeAgenda(fila.profesionalId(), fila.profesionalNombre(), fila.profesionalApellido()),
                new Referencia(fila.sillonId(), fila.sillonNombre()));
    }
}

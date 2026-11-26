package ar.dentaflow.aplicacion.pacientes;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.pacientes.TurnosDelPaciente;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Baja de un paciente por anonimización (C-15, PA-12, design D4). Bloquea la fila del paciente para que una
 * reserva simultánea con su DNI no le cree un turno en el medio, y rechaza la baja si tiene turnos futuros
 * {@code pendiente} o {@code confirmado}: primero hay que cancelarlos. Sus turnos pasados y su historial de
 * auditoría se conservan (retención de 5 años).
 *
 * <p>Retención: no hay purga automática en el MVP. Punto de extensión: anonimizar los pacientes sin turnos en los
 * últimos 5 años, disparado por el mismo mecanismo de tareas que los recordatorios (C-18).
 */
@Service
public class ServicioDeAnonimizacion {

    public static final String TIENE_TURNOS_ACTIVOS =
            "El paciente tiene turnos pendientes o confirmados: cancelalos antes de darlo de baja";

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeAnonimizacion.class);

    private final PacienteRepositorio pacientes;
    private final TurnosDelPaciente turnos;
    private final Clock reloj;

    public ServicioDeAnonimizacion(PacienteRepositorio pacientes, TurnosDelPaciente turnos, Clock reloj) {
        this.pacientes = pacientes;
        this.turnos = turnos;
        this.reloj = reloj;
    }

    /** Anonimiza al paciente; idempotente. Solo ids en el log, nunca datos personales. */
    @Transactional
    public Paciente anonimizar(long pacienteId, long usuarioId) {
        Paciente paciente = pacientes.buscarPorIdParaActualizar(pacienteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el paciente"));
        if (paciente.estaAnonimizado()) {
            return paciente;
        }
        Instant ahora = reloj.instant();
        if (turnos.hayActivosFuturos(pacienteId, ahora)) {
            throw new ConflictoException(TIENE_TURNOS_ACTIVOS);
        }
        paciente.anonimizar(usuarioId, ahora);
        Paciente guardado = pacientes.guardar(paciente);
        LOG.info("Paciente {} anonimizado por el usuario {}", pacienteId, usuarioId);
        return guardado;
    }
}

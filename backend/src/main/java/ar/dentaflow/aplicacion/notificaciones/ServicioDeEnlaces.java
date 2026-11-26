package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.notificaciones.SituacionDeEnlace;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resuelve el token de un enlace del paciente a la situación de su turno (spec enlaces-paciente, design D6). Solo
 * lectura: no cambia el turno ni registra historial. Un token mal formado y uno inexistente dan la misma respuesta.
 *
 * <p><b>Contrato con C-17:</b> antes de confirmar, cancelar o reprogramar, C-17 llama a {@link #resolver} y solo
 * sigue con {@code VIGENTE}; para los demás casos muestra el mensaje explicativo (Flujo 2, casos de error).
 */
@Service
public class ServicioDeEnlaces {

    private final ServicioDeTurnos turnos;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final Clock reloj;

    public ServicioDeEnlaces(ServicioDeTurnos turnos, PrestacionRepositorio prestaciones,
            ProfesionalRepositorio profesionales, Clock reloj) {
        this.turnos = turnos;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public ResolucionDeEnlace resolver(String token) {
        Optional<Turno> encontrado = turnos.buscarPorToken(token);
        if (encontrado.isEmpty()) {
            return ResolucionDeEnlace.inexistente();
        }
        Turno turno = encontrado.get();
        SituacionDeEnlace situacion = SituacionDeEnlace.de(turno, reloj.instant());
        return new ResolucionDeEnlace(situacion, Optional.of(resumen(turno)));
    }

    private ResumenTurnoPaciente resumen(Turno turno) {
        Prestacion prestacion = prestaciones.buscarPorId(turno.getPrestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación del turno no existe"));
        Profesional profesional = profesionales.buscarPorId(turno.getProfesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El profesional del turno no existe"));
        return new ResumenTurnoPaciente(turno.getInicio(), turno.getFin(), turno.getEstado(), prestacion.getNombre(),
                NombresParaElPaciente.de(profesional));
    }
}

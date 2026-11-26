package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.aplicacion.configuracion.ParametrosDelConsultorio;
import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.notificaciones.DatosDelMensajeWhatsapp;
import ar.dentaflow.dominio.notificaciones.EnlaceWhatsapp;
import ar.dentaflow.dominio.notificaciones.PlantillaWhatsapp;
import ar.dentaflow.dominio.notificaciones.TelefonoWhatsapp;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el mensaje de WhatsApp prellenado de un turno (C-21, US-032, RN-NO-04; design D1). Solo lectura: no envía
 * nada, no cambia el turno ni registra historial. Nunca loguea el teléfono, el mensaje, el enlace ni el token.
 */
@Service
public class ServicioDeMensajesWhatsapp {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeMensajesWhatsapp.class);
    private static final Set<EstadoTurno> VIGENTES = Set.of(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO);

    private final ServicioDeTurnos turnos;
    private final PacienteRepositorio pacientes;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final ParametrosDelConsultorio parametros;
    private final EnlacesDeTurno enlaces;

    public ServicioDeMensajesWhatsapp(ServicioDeTurnos turnos, PacienteRepositorio pacientes,
            PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales,
            ParametrosDelConsultorio parametros, EnlacesDeTurno enlaces) {
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.parametros = parametros;
        this.enlaces = enlaces;
    }

    @Transactional(readOnly = true)
    public MensajeWhatsapp prepararPara(long turnoId) {
        Turno turno = turnos.buscarPorId(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
        if (!VIGENTES.contains(turno.getEstado())) {
            throw new ConflictoException("Solo se envían por WhatsApp turnos pendientes o confirmados");
        }
        Paciente paciente = pacientes.buscarPorId(turno.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El paciente del turno no existe"));
        Prestacion prestacion = prestaciones.buscarPorId(turno.getPrestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación del turno no existe"));
        Profesional profesional = profesionales.buscarPorId(turno.getProfesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El profesional del turno no existe"));

        String mensaje = PlantillaWhatsapp.completar(parametros.plantillaWhatsapp(), new DatosDelMensajeWhatsapp(
                paciente.getNombre(), turno.getInicio(), prestacion.getNombre(), NombresParaElPaciente.de(profesional),
                enlaces.confirmar(turno)));
        String enlace = TelefonoWhatsapp.normalizar(paciente.getTelefono())
                .map(telefono -> EnlaceWhatsapp.de(telefono, mensaje))
                .orElse(null);
        LOG.info("Mensaje de WhatsApp preparado para el turno {} ({})", turno.getId(),
                enlace != null ? "con enlace" : "sin teléfono utilizable");
        return new MensajeWhatsapp(mensaje, enlace);
    }
}

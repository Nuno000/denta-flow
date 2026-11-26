package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.aplicacion.agenda.ConsultaDeHuecos;
import ar.dentaflow.aplicacion.agenda.MotorDeDisponibilidad;
import ar.dentaflow.aplicacion.configuracion.ParametrosDelConsultorio;
import ar.dentaflow.aplicacion.notificaciones.ResolucionDeEnlace;
import ar.dentaflow.aplicacion.notificaciones.ResumenTurnoPaciente;
import ar.dentaflow.aplicacion.notificaciones.ServicioDeEnlaces;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.notificaciones.SituacionDeEnlace;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso que gestiona las acciones autónomas del paciente sobre su turno mediante su token (C-17 design D1):
 * consultar estado, confirmar con un toque, cancelar dentro de anticipación y reprogramar con el motor de disponibilidad.
 */
@Service
public class ServicioDeGestionTurnoPaciente {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeGestionTurnoPaciente.class);

    private final ServicioDeEnlaces enlaces;
    private final ServicioDeTurnos turnos;
    private final ParametrosDelConsultorio parametros;
    private final MotorDeDisponibilidad motor;
    private final Clock reloj;

    public ServicioDeGestionTurnoPaciente(ServicioDeEnlaces enlaces, ServicioDeTurnos turnos,
            ParametrosDelConsultorio parametros, MotorDeDisponibilidad motor, Clock reloj) {
        this.enlaces = enlaces;
        this.turnos = turnos;
        this.parametros = parametros;
        this.motor = motor;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public TurnoPublicoDetalle consultarPorToken(String token) {
        ResolucionDeEnlace resolucion = resolverToken(token);
        ResumenTurnoPaciente resumen = resolucion.resumen()
                .orElseThrow(() -> new RecursoNoEncontradoException("El enlace no es válido o ha expirado"));

        boolean puedeModificar = false;
        if (resolucion.situacion() == SituacionDeEnlace.VIGENTE) {
            puedeModificar = estaDentroDeAnticipacion(resumen.inicio());
        }

        return new TurnoPublicoDetalle(
                resolucion.situacion(),
                resumen.inicio(),
                resumen.fin(),
                resumen.prestacion(),
                resumen.profesional(),
                resumen.estado().codigo(),
                puedeModificar);
    }

    public TurnoPublicoDetalle confirmar(String token) {
        ResolucionDeEnlace resolucion = resolverToken(token);
        validarVigenciaParaAccion(resolucion.situacion(), "confirmar");

        Turno turno = buscarTurno(token);
        if (turno.getEstado() == EstadoTurno.CONFIRMADO) {
            // Idempotente: ya confirmado
            return consultarPorToken(token);
        }
        if (turno.getEstado() != EstadoTurno.PENDIENTE) {
            throw new ConflictoException("El turno no se encuentra en un estado que permita su confirmación");
        }

        turnos.confirmar(turno.getId(), Actor.paciente());
        LOG.info("Turno {} confirmado por el paciente", turno.getId());
        return consultarPorToken(token);
    }

    public TurnoPublicoDetalle cancelar(String token) {
        ResolucionDeEnlace resolucion = resolverToken(token);
        validarVigenciaParaAccion(resolucion.situacion(), "cancelar");

        Turno turno = buscarTurno(token);
        exigirAnticipacionMinima(turno.getInicio());

        turnos.cancelar(turno.getId(), Actor.paciente());
        LOG.info("Turno {} cancelado por el paciente", turno.getId());
        return consultarPorToken(token);
    }

    public List<Hueco> consultarHuecosParaReprogramar(String token, LocalDate desde, LocalDate hasta) {
        ResolucionDeEnlace resolucion = resolverToken(token);
        validarVigenciaParaAccion(resolucion.situacion(), "reprogramar");

        Turno turno = buscarTurno(token);
        exigirAnticipacionMinima(turno.getInicio());

        return motor.buscarHuecos(new ConsultaDeHuecos(
                turno.getPrestacionId(),
                turno.getProfesionalId(),
                desde,
                hasta,
                turno.getId()));
    }

    public TurnoPublicoDetalle reprogramar(String token, Instant nuevoInicio) {
        ResolucionDeEnlace resolucion = resolverToken(token);
        validarVigenciaParaAccion(resolucion.situacion(), "reprogramar");

        Turno turno = buscarTurno(token);
        exigirAnticipacionMinima(turno.getInicio());

        List<CandidatoDeReserva> candidatos = motor.candidatosPara(
                turno.getPrestacionId(),
                turno.getProfesionalId(),
                nuevoInicio,
                turno.getId());

        if (candidatos.isEmpty()) {
            throw new HuecoNoDisponibleException("El horario elegido ya no está disponible; elegí otro");
        }

        CandidatoDeReserva candidato = candidatos.getFirst();
        turnos.reprogramar(turno.getId(),
                new SolicitudDeReprogramacion(nuevoInicio, candidato.profesionalId(), candidato.sillonId()),
                Actor.paciente());

        LOG.info("Turno {} reprogramado por el paciente al horario {}", turno.getId(), nuevoInicio);
        return consultarPorToken(token);
    }

    private ResolucionDeEnlace resolverToken(String token) {
        ResolucionDeEnlace resolucion = enlaces.resolver(token);
        if (resolucion.situacion() == SituacionDeEnlace.INEXISTENTE) {
            throw new RecursoNoEncontradoException("El enlace no es válido o ha expirado");
        }
        return resolucion;
    }

    private Turno buscarTurno(String token) {
        return turnos.buscarPorToken(token)
                .orElseThrow(() -> new RecursoNoEncontradoException("El enlace no es válido o ha expirado"));
    }

    private void validarVigenciaParaAccion(SituacionDeEnlace situacion, String accion) {
        if (situacion == SituacionDeEnlace.CANCELADO) {
            throw new ConflictoException("El turno ya se encuentra cancelado");
        }
        if (situacion == SituacionDeEnlace.VENCIDO) {
            throw new ConflictoException("El turno ya ha vencido o fue atendido");
        }
        if (situacion != SituacionDeEnlace.VIGENTE) {
            throw new ConflictoException("No es posible " + accion + " el turno en su situación actual");
        }
    }

    private boolean estaDentroDeAnticipacion(Instant inicio) {
        Duration anticipacionMinima = parametros.anticipacionMinima();
        Instant limite = reloj.instant().plus(anticipacionMinima);
        return !limite.isAfter(inicio);
    }

    private void exigirAnticipacionMinima(Instant inicio) {
        if (!estaDentroDeAnticipacion(inicio)) {
            throw CancelacionFueraDeTerminoException.conContacto(parametros.contactoConsultorio());
        }
    }
}

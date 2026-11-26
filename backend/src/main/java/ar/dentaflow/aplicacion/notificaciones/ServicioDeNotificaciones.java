package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import ar.dentaflow.dominio.notificaciones.FallaDeEnvio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import ar.dentaflow.dominio.notificaciones.Notificacion;
import ar.dentaflow.dominio.notificaciones.NotificacionRepositorio;
import ar.dentaflow.dominio.notificaciones.PoliticaDeReintento;
import ar.dentaflow.dominio.notificaciones.TipoNotificacion;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía el email de confirmación de reserva y registra el resultado (spec notificaciones-email, design D5).
 *
 * <p><b>Contrato con C-16:</b> llamar a {@link #enviarConfirmacion} cuando la reserva ya está confirmada. Si se
 * llama dentro de una transacción, el envío se posterga hasta su commit (y no ocurre si se revierte). Vuelve
 * enseguida: el envío corre en {@link EjecutorDeEmails}, nunca en el hilo de la request ni dentro de una
 * transacción, y una falla de envío no toca el turno.
 *
 * <p>Pasos: (1) transacción corta que registra la notificación {@code en_curso}, u {@code omitida} si el paciente
 * está anonimizado (C-15); si ya existía, no hace nada; (2) hasta {@value PoliticaDeReintento#INTENTOS_MAXIMOS}
 * intentos fuera de transacción; (3) transacción corta con el resultado. Si el proceso se cae entre (1) y (3), la
 * notificación queda {@code en_curso} y la retoma C-18. Los logs llevan solo ids, estado, intentos y motivo.
 */
@Service
public class ServicioDeNotificaciones {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeNotificaciones.class);
    private static final TipoNotificacion TIPO = TipoNotificacion.CONFIRMACION;

    private final NotificacionRepositorio notificaciones;
    private final ServicioDeTurnos turnos;
    private final PacienteRepositorio pacientes;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final EnviadorDeEmail enviador;
    private final EnlacesDeTurno enlaces;
    private final PlantillaConfirmacionReserva plantilla;
    private final EjecutorDeEmails ejecutor;
    private final PropiedadesEnvio propiedades;
    private final TransactionTemplate transaccion;
    private final Clock reloj;
    private final PoliticaDeReintento politica = new PoliticaDeReintento();

    public ServicioDeNotificaciones(NotificacionRepositorio notificaciones, ServicioDeTurnos turnos,
            PacienteRepositorio pacientes, PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales,
            EnviadorDeEmail enviador, EnlacesDeTurno enlaces, PlantillaConfirmacionReserva plantilla,
            EjecutorDeEmails ejecutor, PropiedadesEnvio propiedades, TransactionTemplate transaccion, Clock reloj) {
        this.notificaciones = notificaciones;
        this.turnos = turnos;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.enviador = enviador;
        this.enlaces = enlaces;
        this.plantilla = plantilla;
        this.ejecutor = ejecutor;
        this.propiedades = propiedades;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    /** Pide el email de confirmación del turno. Idempotente: un turno recibe una sola confirmación. */
    public void enviarConfirmacion(long turnoId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    programar(turnoId);
                }
            });
            return;
        }
        programar(turnoId);
    }

    private void programar(long turnoId) {
        try {
            ejecutor.ejecutar(() -> enviarAhora(turnoId));
        } catch (RejectedExecutionException e) {
            LOG.warn("turno={} tipo={}: cola de envío llena, la confirmación no se registró", turnoId,
                    TIPO.codigo());
        }
    }

    private void enviarAhora(long turnoId) {
        try {
            preparar(turnoId).ifPresent(this::entregar);
        } catch (RuntimeException e) {
            LOG.error("turno={} tipo={}: error inesperado al enviar ({})", turnoId, TIPO.codigo(),
                    e.getClass().getSimpleName());
        }
    }

    private record Preparado(Notificacion notificacion, MensajeEmail mensaje) {
    }

    private Optional<Preparado> preparar(long turnoId) {
        try {
            return Optional.ofNullable(transaccion.execute(estado -> registrar(turnoId)));
        } catch (DataIntegrityViolationException e) {
            LOG.info("turno={} tipo={}: ya registrada por otra ejecución", turnoId, TIPO.codigo());
            return Optional.empty();
        }
    }

    private Preparado registrar(long turnoId) {
        if (notificaciones.buscarPorTurnoYTipo(turnoId, TIPO).isPresent()) {
            return null;
        }
        Turno turno = turnos.buscarPorId(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
        Paciente paciente = pacientes.buscarPorId(turno.getPacienteId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El paciente del turno no existe"));
        Notificacion notificacion = Notificacion.nueva(turnoId, TIPO, reloj.instant());
        if (!paciente.puedeRecibirEmails()) {
            notificacion.omitir(reloj.instant());
            registrarEnLog(notificaciones.guardar(notificacion));
            return null;
        }
        Prestacion prestacion = prestaciones.buscarPorId(turno.getPrestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación del turno no existe"));
        Profesional profesional = profesionales.buscarPorId(turno.getProfesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("El profesional del turno no existe"));
        Notificacion guardada = notificaciones.guardar(notificacion);
        MensajeEmail mensaje = plantilla.armar(new DatosDelEmail(paciente.getEmail(), paciente.getNombre(),
                turno.getInicio(), prestacion.getNombre(), NombresParaElPaciente.de(profesional),
                enlaces.todos(turno)));
        return new Preparado(guardada, mensaje);
    }

    private void entregar(Preparado preparado) {
        Notificacion notificacion = preparado.notificacion();
        MotivoFallo motivo;
        while (true) {
            notificacion.registrarIntento(reloj.instant());
            motivo = intentar(preparado.mensaje());
            if (motivo == null || !politica.debeReintentar(notificacion.getIntentos(), motivo)) {
                break;
            }
            esperarAntesDelIntento(notificacion.getIntentos() + 1);
        }
        if (motivo == null) {
            notificacion.marcarEnviada(reloj.instant());
        } else {
            notificacion.marcarFallida(motivo, reloj.instant());
        }
        registrarEnLog(transaccion.execute(estado -> notificaciones.guardar(notificacion)));
    }

    /** Un intento de envío: {@code null} si salió, o el motivo de la falla. */
    private MotivoFallo intentar(MensajeEmail mensaje) {
        try {
            enviador.enviar(mensaje);
            return null;
        } catch (FallaDeEnvio falla) {
            return falla.motivo();
        } catch (RuntimeException e) {
            return MotivoFallo.DESCONOCIDO;
        }
    }

    private void esperarAntesDelIntento(int intento) {
        Duration espera = propiedades.esperaEntreIntentos().multipliedBy(1L << (intento - 2));
        if (espera.isZero()) {
            return;
        }
        try {
            Thread.sleep(espera.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void registrarEnLog(Notificacion n) {
        if (n.getMotivoFallo() != null) {
            LOG.warn("notificacion={} turno={} tipo={} estado={} intentos={} motivo={}", n.getId(), n.getTurnoId(),
                    n.getTipo().codigo(), n.getEstado().codigo(), n.getIntentos(), n.getMotivoFallo().codigo());
        } else {
            LOG.info("notificacion={} turno={} tipo={} estado={} intentos={}", n.getId(), n.getTurnoId(),
                    n.getTipo().codigo(), n.getEstado().codigo(), n.getIntentos());
        }
    }
}

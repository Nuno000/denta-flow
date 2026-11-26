package ar.dentaflow.aplicacion.reservas;

import ar.dentaflow.aplicacion.agenda.ServicioDeReservaDeHuecos;
import ar.dentaflow.aplicacion.agenda.SolicitudDeReservaDeHueco;
import ar.dentaflow.aplicacion.notificaciones.ServicioDeNotificaciones;
import ar.dentaflow.aplicacion.pacientes.DatosPaciente;
import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Caso de uso: reserva pública online (C-16 design D2).
 * <p>
 * <b>Sin @Transactional:</b> cada paso es atómico en su propia transacción (registro de paciente en
 * {@link ServicioDePacientes}, asignación y creación pesimista en {@link ServicioDeReservaDeHuecos}).
 * La notificación transaccional asíncrona corre al final. Ningún dato personal ni IP se loguea.
 */
@Service
public class ServicioDeReservaOnline {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeReservaOnline.class);

    private final LimitadorDeReservasPorOrigen limitador;
    private final ServicioDePacientes pacientes;
    private final ServicioDeReservaDeHuecos reservaDeHuecos;
    private final ServicioDeNotificaciones notificaciones;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final Clock reloj;

    public ServicioDeReservaOnline(
            LimitadorDeReservasPorOrigen limitador,
            ServicioDePacientes pacientes,
            ServicioDeReservaDeHuecos reservaDeHuecos,
            ServicioDeNotificaciones notificaciones,
            PrestacionRepositorio prestaciones,
            ProfesionalRepositorio profesionales,
            Clock reloj) {
        this.limitador = limitador;
        this.pacientes = pacientes;
        this.reservaDeHuecos = reservaDeHuecos;
        this.notificaciones = notificaciones;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.reloj = reloj;
    }

    public ReservaConfirmada reservar(SolicitudDeReservaOnline solicitud) {
        // 1. Límite anti-abuso por origen
        limitador.registrarPedido(solicitud.origenIp());

        // 2. Validación de aviso de privacidad
        if (!Boolean.TRUE.equals(solicitud.aceptaAvisoPrivacidad())) {
            throw new ValidacionDeDominioException(ServicioDePacientes.AVISO_NO_ACEPTADO);
        }

        // 3. Alta / lectura deduplicada del paciente (tx propia, registra consentimiento)
        Paciente paciente = pacientes.registrarDesdeReservaOnline(new DatosPaciente(
                solicitud.dni(),
                solicitud.nombre(),
                solicitud.apellido(),
                solicitud.telefono(),
                solicitud.email(),
                reloj.instant()));

        // 4. Reserva atómica del hueco (prueba candidatos, FOR UPDATE profesional -> sillón)
        TurnoCreado creado = reservaDeHuecos.reservar(new SolicitudDeReservaDeHueco(
                paciente.getId(),
                solicitud.prestacionId(),
                solicitud.profesionalId(),
                solicitud.inicio(),
                OrigenTurno.ONLINE,
                Actor.paciente()));

        // 5. Envío asíncrono de confirmación con enlaces no adivinables
        notificaciones.enviarConfirmacion(creado.turno().getId());

        LOG.info("Reserva online exitosa (turnoId={}, prestacionId={}, profesionalId={})",
                creado.turno().getId(), creado.turno().getPrestacionId(), creado.turno().getProfesionalId());

        // 6. Resumen sin datos personales, sillón ni token
        Prestacion prestacion = prestaciones.buscarPorId(creado.turno().getPrestacionId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Prestación no encontrada"));
        Profesional profesional = profesionales.buscarPorId(creado.turno().getProfesionalId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Profesional no encontrado"));

        return new ReservaConfirmada(
                creado.turno().getInicio(),
                creado.turno().getFin(),
                prestacion.getNombre(),
                profesional.getNombre() + " " + profesional.getApellido(),
                creado.turno().getEstado());
    }
}

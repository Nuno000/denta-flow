package ar.dentaflow.aplicacion.listaespera;

import ar.dentaflow.aplicacion.agenda.MotorDeDisponibilidad;
import ar.dentaflow.aplicacion.configuracion.ParametrosDelConsultorio;
import ar.dentaflow.aplicacion.notificaciones.ServicioDeNotificacionesListaEspera;
import ar.dentaflow.aplicacion.turnos.TurnoCanceladoEvent;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.configuracion.ClaveParametro;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.GeneradorDeTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class OferenteDeListaDeEspera {

    private static final Logger LOG = LoggerFactory.getLogger(OferenteDeListaDeEspera.class);

    private final ListaEsperaRepositorio listaEspera;
    private final MotorDeDisponibilidad motor;
    private final PacienteRepositorio pacientes;
    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;
    private final ParametrosDelConsultorio parametros;
    private final GeneradorDeTokens generadorDeTokens;
    private final ServicioDeNotificacionesListaEspera notificaciones;
    private final Clock reloj;

    public OferenteDeListaDeEspera(ListaEsperaRepositorio listaEspera, MotorDeDisponibilidad motor,
            PacienteRepositorio pacientes, PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales,
            ParametrosDelConsultorio parametros, GeneradorDeTokens generadorDeTokens,
            ServicioDeNotificacionesListaEspera notificaciones, Clock reloj) {
        this.listaEspera = listaEspera;
        this.motor = motor;
        this.pacientes = pacientes;
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
        this.parametros = parametros;
        this.generadorDeTokens = generadorDeTokens;
        this.notificaciones = notificaciones;
        this.reloj = reloj;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void alCancelarTurno(TurnoCanceladoEvent evento) {
        Instant ahora = reloj.instant();
        if (!evento.inicio().isAfter(ahora)) {
            return;
        }
        if (motor.candidatosPara(evento.prestacionId(), evento.profesionalId(), evento.inicio(), null).isEmpty()) {
            return;
        }

        ZonedDateTime inicioLocal = evento.inicio().atZone(ZonaHorariaConsultorio.ZONA);
        LocalDate fechaLocal = inicioLocal.toLocalDate();
        LocalTime horaLocal = inicioLocal.toLocalTime();

        List<ListaEspera> candidatas = listaEspera.buscarActivasCompatibles(
                evento.prestacionId(), evento.profesionalId(), fechaLocal);
        if (candidatas.isEmpty()) {
            return;
        }

        Instant vence = ahora.plus(parametros.plazoOfertaListaEspera());

        Prestacion prestacion = prestaciones.buscarPorId(evento.prestacionId()).orElse(null);
        Profesional profesional = profesionales.buscarPorId(evento.profesionalId()).orElse(null);
        if (prestacion == null || profesional == null) {
            return;
        }
        String nombreProfesional = profesional.getNombre() + " " + profesional.getApellido();

        for (ListaEspera entrada : candidatas) {
            if (!entrada.esCompatibleCon(evento.prestacionId(), evento.profesionalId(), fechaLocal, horaLocal)) {
                continue;
            }
            Paciente paciente = pacientes.buscarPorId(entrada.getPacienteId()).orElse(null);
            if (paciente == null || paciente.estaAnonimizado()) {
                continue;
            }

            String token = generadorDeTokens.nuevoToken();
            entrada.ofrecer(evento.turnoId(), evento.profesionalId(), evento.inicio(), token, ahora, vence);
            listaEspera.guardar(entrada);

            notificaciones.enviarOferta(
                    entrada.getId(),
                    paciente.getEmail(),
                    paciente.getNombre(),
                    prestacion.getNombre(),
                    nombreProfesional,
                    evento.inicio(),
                    vence,
                    token);
        }
        LOG.info("Ofertas de lista de espera procesadas para turno cancelado {}", evento.turnoId());
    }
}

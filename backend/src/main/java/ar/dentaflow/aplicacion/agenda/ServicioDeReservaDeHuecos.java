package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Confirma un hueco probando cada candidato en su propia transacción de {@link ServicioDeTurnos}
 * (DD-09, design D6). No lleva {@code @Transactional} para no retener bloqueos entre intentos ni
 * violar la guarda de aislamiento de C-09.
 */
@Service
public class ServicioDeReservaDeHuecos {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeReservaDeHuecos.class);

    private final MotorDeDisponibilidad motor;
    private final ServicioDeTurnos turnos;

    public ServicioDeReservaDeHuecos(MotorDeDisponibilidad motor, ServicioDeTurnos turnos) {
        this.motor = motor;
        this.turnos = turnos;
    }

    /** Recalcula candidatos del instante y crea el turno con el primero que logre confirmarse. */
    public TurnoCreado reservar(SolicitudDeReservaDeHueco solicitud) {
        List<CandidatoDeReserva> candidatos = motor.candidatosPara(
                solicitud.prestacionId(), solicitud.profesionalId(), solicitud.inicio(), null);
        int intentos = 0;
        for (CandidatoDeReserva candidato : candidatos) {
            intentos++;
            try {
                return turnos.crear(new SolicitudDeTurno(
                        solicitud.pacienteId(),
                        candidato.profesionalId(),
                        candidato.sillonId(),
                        solicitud.prestacionId(),
                        solicitud.inicio(),
                        solicitud.origen(),
                        false,
                        solicitud.actor()));
            } catch (TurnoSolapadoException | AgendaNoDisponibleException | PessimisticLockingFailureException ex) {
                LOG.debug("Candidato descartado al reservar hueco (intento {}): {}", intentos, ex.getClass().getSimpleName());
            }
        }
        LOG.info("Hueco no disponible tras {} intentos (prestacion={}, profesional={})",
                intentos, solicitud.prestacionId(), solicitud.profesionalId());
        throw new HuecoNoDisponibleException("El horario elegido ya no está disponible; elegí otro");
    }
}

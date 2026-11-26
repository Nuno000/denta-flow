package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.aplicacion.pacientes.DatosPaciente;
import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import org.springframework.stereotype.Service;

/**
 * Carga de turnos y sobreturnos por el personal (C-12, US-021 y US-022; design D2). Registra al paciente por DNI con
 * la política de recepción y crea el turno por la única vía de escritura, {@link ServicioDeTurnos#crear}.
 *
 * <p><b>Contrato:</b> no lleva transacción propia (como {@code ServicioDeReservaDeHuecos}): {@code ServicioDeTurnos}
 * debe ser el borde transaccional ({@code READ COMMITTED}) y el paciente se registra en <b>otra</b> transacción antes,
 * así que <b>queda registrado aunque el turno se rechace</b> (conflicto, recurso inexistente o inicio pasado). El rol
 * (administrador o recepción) lo verifica quien llama. Los errores de bloqueo de la base se propagan sin reintento.
 * Nada de lo que recibe se escribe en los logs.
 */
@Service
public class CargaManualDeTurnos {

    private final ServicioDePacientes pacientes;
    private final ServicioDeTurnos turnos;
    private final SugeridorDeAlternativas sugeridor;

    public CargaManualDeTurnos(ServicioDePacientes pacientes, ServicioDeTurnos turnos,
            SugeridorDeAlternativas sugeridor) {
        this.pacientes = pacientes;
        this.turnos = turnos;
        this.sugeridor = sugeridor;
    }

    /**
     * Turno común: nunca es sobreturno. Si choca por superposición, horario o bloqueo lanza
     * {@link ConflictoConAlternativasException} con el mensaje original y horarios libres cercanos.
     */
    public TurnoCreado cargar(PedidoDeCargaManual pedido, long usuarioId) {
        try {
            return crear(pedido, usuarioId, false);
        } catch (TurnoSolapadoException | AgendaNoDisponibleException conflicto) {
            throw new ConflictoConAlternativasException(conflicto.getMessage(), sugeridor.sugerir(
                    pedido.prestacionId(), pedido.profesionalId(), pedido.sillonId(), pedido.inicio()));
        }
    }

    /**
     * Sobreturno: se superpone con otros turnos pero sigue respetando horario y bloqueos; si choca con ellos la
     * excepción original se propaga sin alternativas.
     */
    public TurnoCreado cargarSobreturno(PedidoDeCargaManual pedido, long usuarioId) {
        return crear(pedido, usuarioId, true);
    }

    private TurnoCreado crear(PedidoDeCargaManual pedido, long usuarioId, boolean esSobreturno) {
        Paciente paciente = pacientes.registrarDesdeRecepcion(new DatosPaciente(pedido.dni(), pedido.nombre(),
                pedido.apellido(), pedido.telefono(), pedido.email(), null));
        return turnos.crear(new SolicitudDeTurno(paciente.getId(), pedido.profesionalId(), pedido.sillonId(),
                pedido.prestacionId(), pedido.inicio(), OrigenTurno.RECEPCION, esSobreturno,
                Actor.usuario(usuarioId)));
    }
}

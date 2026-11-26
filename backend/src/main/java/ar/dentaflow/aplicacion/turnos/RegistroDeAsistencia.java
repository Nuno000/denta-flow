package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Registro de asistencia de un turno (C-13, US-024; spec asistencia-turnos; design D2). Verifica el estado pedido,
 * la regla de "propios" del profesional y que el turno ya haya empezado (salvo "ausente con aviso"), y delega en
 * {@link ServicioDeTurnos#cambiarEstado}, la única vía de escritura: valida la transición (RN-TU-05), bloquea la
 * fila y audita en la misma transacción (RN-AU-01).
 *
 * <p>No abre transacción propia: {@code ServicioDeTurnos} exige ser el borde transaccional con {@code READ COMMITTED}.
 * Las verificaciones usan una lectura previa sin bloqueo (riesgo aceptado en design D2).
 */
@Service
public class RegistroDeAsistencia {

    private static final Set<EstadoTurno> ASISTENCIA =
            EnumSet.of(EstadoTurno.ASISTIO, EstadoTurno.AUSENTE, EstadoTurno.AUSENTE_CON_AVISO);

    /** Solo estos necesitan que el turno haya empezado; el aviso de ausencia puede llegar antes. */
    private static final Set<EstadoTurno> DESDE_EL_INICIO = EnumSet.of(EstadoTurno.ASISTIO, EstadoTurno.AUSENTE);

    private final ServicioDeTurnos turnos;
    private final Clock reloj;

    public RegistroDeAsistencia(ServicioDeTurnos turnos, Clock reloj) {
        this.turnos = turnos;
        this.reloj = reloj;
    }

    public Turno registrar(long turnoId, String codigoEstado, Rol rol, Long usuarioId, Long profesionalDelUsuario) {
        EstadoTurno estado = estadoDeAsistencia(codigoEstado);
        Turno turno = turnos.buscarPorId(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
        verificarPropio(turno, rol, profesionalDelUsuario);
        if (DESDE_EL_INICIO.contains(estado) && reloj.instant().isBefore(turno.getInicio())) {
            throw new OperacionNoPermitidaException("La asistencia se registra desde el inicio del turno");
        }
        return turnos.cambiarEstado(turnoId, estado, Actor.usuario(usuarioId));
    }

    /** El mensaje no repite el valor recibido (C-15, design D7: errores sin datos del input). */
    private static EstadoTurno estadoDeAsistencia(String codigo) {
        return ASISTENCIA.stream()
                .filter(estado -> estado.codigo().equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException(
                        "La asistencia debe ser asistio, ausente o ausente_con_aviso"));
    }

    private static void verificarPropio(Turno turno, Rol rol, Long profesionalDelUsuario) {
        if (rol != Rol.PROFESIONAL) {
            return;
        }
        if (profesionalDelUsuario == null || !profesionalDelUsuario.equals(turno.getProfesionalId())) {
            throw new AccessDeniedException("Solo podés registrar la asistencia de tus turnos");
        }
    }
}

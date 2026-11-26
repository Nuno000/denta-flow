package ar.dentaflow.aplicacion.auditoria;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.TipoDeActor;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta del historial de cambios de un turno para el administrador (C-13, US-025; spec auditoria-turnos; design D1).
 * Solo lectura: verifica que el turno exista, lee los registros en orden cronológico y resuelve el email de los
 * usuarios internos involucrados sin repetir consultas por el mismo id.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaDeHistorialDeTurno {

    private final ServicioDeTurnos turnos;
    private final ServicioDeAuditoria auditoria;
    private final UsuarioRepositorio usuarios;

    public ConsultaDeHistorialDeTurno(ServicioDeTurnos turnos, ServicioDeAuditoria auditoria,
            UsuarioRepositorio usuarios) {
        this.turnos = turnos;
        this.auditoria = auditoria;
        this.usuarios = usuarios;
    }

    public List<EntradaDeHistorial> consultar(long turnoId) {
        turnos.buscarPorId(turnoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("El turno no existe"));
        List<CambioDeTurno> cambios = auditoria.historialDe(turnoId);
        Map<Long, String> emailsPorUsuario = resolverEmails(cambios);
        return cambios.stream()
                .map(cambio -> aEntrada(cambio, emailsPorUsuario))
                .toList();
    }

    private Map<Long, String> resolverEmails(List<CambioDeTurno> cambios) {
        Map<Long, String> emails = new HashMap<>();
        cambios.stream()
                .filter(cambio -> cambio.actor().tipo() == TipoDeActor.USUARIO && cambio.actor().usuarioId() != null)
                .map(cambio -> cambio.actor().usuarioId())
                .distinct()
                .forEach(usuarioId -> usuarios.buscarPorId(usuarioId)
                        .map(Usuario::getEmail)
                        .ifPresent(email -> emails.put(usuarioId, email)));
        return emails;
    }

    private static EntradaDeHistorial aEntrada(CambioDeTurno cambio, Map<Long, String> emailsPorUsuario) {
        TipoDeActor tipo = cambio.actor().tipo();
        String actorEmail = tipo == TipoDeActor.USUARIO ? emailsPorUsuario.get(cambio.actor().usuarioId()) : null;
        return new EntradaDeHistorial(
                cambio.id(),
                cambio.ocurridoEn(),
                tipo,
                actorEmail,
                cambio.accion(),
                cambio.valorAnterior(),
                cambio.valorNuevo());
    }
}

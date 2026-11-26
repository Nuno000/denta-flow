package ar.dentaflow.infraestructura.persistencia.notificaciones;

import ar.dentaflow.dominio.notificaciones.Notificacion;
import ar.dentaflow.dominio.notificaciones.NotificacionRepositorio;
import ar.dentaflow.dominio.notificaciones.TipoNotificacion;
import java.util.Optional;
import org.springframework.data.repository.Repository;

/**
 * Implementación Spring Data del puerto {@link NotificacionRepositorio}. Extiende {@link Repository} y no
 * {@code CrudRepository}: sin {@code delete*}.
 */
public interface NotificacionRepositorioJpa extends Repository<Notificacion, Long>, NotificacionRepositorio {

    Notificacion save(Notificacion notificacion);

    Optional<Notificacion> findById(Long id);

    Optional<Notificacion> findByTurnoIdAndTipo(Long turnoId, TipoNotificacion tipo);

    @Override
    default Notificacion guardar(Notificacion notificacion) {
        return save(notificacion);
    }

    @Override
    default Optional<Notificacion> buscarPorId(Long id) {
        return findById(id);
    }

    @Override
    default Optional<Notificacion> buscarPorTurnoYTipo(Long turnoId, TipoNotificacion tipo) {
        return findByTurnoIdAndTipo(turnoId, tipo);
    }
}

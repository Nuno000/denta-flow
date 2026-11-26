package ar.dentaflow.dominio.notificaciones;

import java.util.Optional;

/**
 * Puerto de persistencia de notificaciones. Sin borrado: el registro del envío es evidencia. La base garantiza una
 * sola notificación por turno y tipo ({@code uk_notificacion_turno_tipo}).
 */
public interface NotificacionRepositorio {

    Notificacion guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(Long id);

    Optional<Notificacion> buscarPorTurnoYTipo(Long turnoId, TipoNotificacion tipo);
}

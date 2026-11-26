package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import java.util.List;

/**
 * Puerto del aviso de PA-11: turnos activos que se superponen con la franja y el ámbito de un
 * bloqueo, ordenados por inicio. Solo consulta; nunca cancela ni mueve turnos. C-09 lo implementa
 * sobre la tabla {@code turno}; hasta entonces rige {@link SinTurnosAfectados}.
 */
public interface TurnosAfectadosPorBloqueo {

    List<TurnoAfectado> listar(Bloqueo bloqueo);
}

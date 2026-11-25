package ar.dentaflow.dominio.auditoria;

import java.util.List;

/**
 * Puerto del historial de turnos. Solo agrega y consulta: no hay edición ni borrado de registros
 * (spec "Historial solo de inserción", design D4).
 */
public interface HistorialDeTurnoRepositorio {

    /** Guarda un registro nuevo (sin id) y lo devuelve con su id. Rechaza un registro que ya tiene id. */
    CambioDeTurno agregar(CambioDeTurno cambio);

    /** Historial de un turno, del cambio más antiguo al más reciente; a igual instante, por orden de alta. */
    List<CambioDeTurno> historialDe(long turnoId);
}

package ar.dentaflow.aplicacion.auditoria;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.HistorialDeTurnoRepositorio;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auditoría de los cambios de turno (RN-AU-01, design D5). Contrato para C-09, C-11, C-12 y C-13:
 * {@link #registrarCambio} se llama dentro de la transacción del cambio auditado, después de tomar los
 * bloqueos; el registro se confirma o se revierte con ese cambio. Quien llama arma el {@link Actor}
 * (design D6); la fecha y hora sale siempre del reloj del sistema.
 */
@Service
public class ServicioDeAuditoria {

    private final HistorialDeTurnoRepositorio historial;
    private final Clock reloj;

    public ServicioDeAuditoria(HistorialDeTurnoRepositorio historial, Clock reloj) {
        this.historial = historial;
        this.reloj = reloj;
    }

    /**
     * Registra un cambio de turno. Exige una transacción abierta ({@code MANDATORY}): sin ella falla y no
     * escribe nada. Los errores (actor inexistente, valores demasiado largos) salen de acá y no al
     * confirmar, porque el repositorio escribe en el momento.
     *
     * @param valorAnterior campos que cambiaron con su valor previo; vacío o {@code null} solo en la creación
     * @param valorNuevo campos que cambiaron con su valor nuevo; instantes en UTC ISO-8601
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public CambioDeTurno registrarCambio(long turnoId, AccionDeTurno accion, Actor actor,
            Map<String, String> valorAnterior, Map<String, String> valorNuevo) {
        CambioDeTurno cambio = CambioDeTurno.registrar(turnoId, actor, reloj.instant(), accion, valorAnterior,
                valorNuevo);
        return historial.agregar(cambio);
    }

    /** Historial del turno, del cambio más antiguo al más reciente. Vacío si no tiene cambios. */
    @Transactional(readOnly = true)
    public List<CambioDeTurno> historialDe(long turnoId) {
        return historial.historialDe(turnoId);
    }
}

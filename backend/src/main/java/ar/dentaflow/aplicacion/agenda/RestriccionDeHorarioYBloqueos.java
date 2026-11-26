package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.aplicacion.disponibilidad.DisponibilidadDeRecursos;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.turnos.RestriccionDeAgenda;
import ar.dentaflow.dominio.turnos.TurnoPropuesto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Verificación final de horarios y bloqueos dentro de la transacción de {@code ServicioDeTurnos}
 * (RN-TU-04, design D5). Se evalúa con los bloqueos de fila ya tomados sobre profesional y sillón,
 * también para sobreturnos.
 */
@Component
@Order(0)
public class RestriccionDeHorarioYBloqueos implements RestriccionDeAgenda {

    private final DisponibilidadDeRecursos disponibilidad;

    public RestriccionDeHorarioYBloqueos(DisponibilidadDeRecursos disponibilidad) {
        this.disponibilidad = disponibilidad;
    }

    @Override
    public void verificar(TurnoPropuesto propuesto) {
        if (!disponibilidad.profesionalDisponible(propuesto.profesionalId(), propuesto.inicio(), propuesto.fin())) {
            throw new AgendaNoDisponibleException("El profesional no atiende o tiene un bloqueo en ese horario");
        }
        if (!disponibilidad.sillonDisponible(propuesto.sillonId(), propuesto.inicio(), propuesto.fin())) {
            throw new AgendaNoDisponibleException("El sillón tiene un bloqueo en ese horario");
        }
    }
}

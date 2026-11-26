package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.turnos.OcupacionDeRecurso;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Datos ya leídos para calcular huecos en un rango de fechas del consultorio sin tocar la base.
 * Profesionales y sillones conservan el orden de prioridad de asignación.
 */
public record AgendaDelRango(
        int duracionMinutos,
        LocalDate desde,
        LocalDate hasta,
        Instant ahora,
        int pasoMinutos,
        List<ProfesionalEnAgenda> profesionales,
        List<SillonEnAgenda> sillones,
        List<OcupacionDeRecurso> ocupacionesProfesionales,
        List<OcupacionDeRecurso> ocupacionesSillones) {

    public record ProfesionalEnAgenda(
            Long id,
            boolean activo,
            List<HorarioAtencion> horarios,
            List<Bloqueo> bloqueos) {
    }

    public record SillonEnAgenda(
            Long id,
            boolean activo,
            List<Bloqueo> bloqueos) {
    }
}

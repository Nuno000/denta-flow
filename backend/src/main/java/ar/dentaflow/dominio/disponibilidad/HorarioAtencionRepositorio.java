package ar.dentaflow.dominio.disponibilidad;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia de los horarios de atención. Los rangos se pueden borrar. */
public interface HorarioAtencionRepositorio {

    HorarioAtencion guardar(HorarioAtencion horario);

    Optional<HorarioAtencion> buscarPorId(Long id);

    /** Rangos del profesional ordenados por día (lunes primero) y hora de inicio. */
    List<HorarioAtencion> listarDelProfesional(Long profesionalId);

    /** Rangos del profesional en un día, ordenados por hora de inicio. */
    List<HorarioAtencion> listarDelProfesionalYDia(Long profesionalId, DayOfWeek dia);

    void eliminar(HorarioAtencion horario);
}

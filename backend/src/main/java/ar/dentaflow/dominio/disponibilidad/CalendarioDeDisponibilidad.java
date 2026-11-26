package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Regla única de disponibilidad de un recurso (RN-TU-04): horario de atención menos bloqueos.
 * Recibe los datos ya leídos y no toca la base. Día y hora se evalúan en la zona del consultorio
 * (SU-09), con intervalos semiabiertos {@code [inicio, fin)}; los rangos contiguos de un día son
 * continuos. Los sillones no tienen horario propio.
 */
public final class CalendarioDeDisponibilidad {

    private CalendarioDeDisponibilidad() {
    }

    /**
     * El profesional está activo, el rango cae en un único día del consultorio, queda contenido en
     * una franja de atención de ese día y no lo toca ningún bloqueo suyo ni del consultorio.
     */
    public static boolean profesionalDisponible(Long profesionalId, boolean activo,
            Collection<HorarioAtencion> horarios, Collection<Bloqueo> bloqueos, Instant inicio, Instant fin) {
        validarRango(inicio, fin);
        if (!activo) {
            return false;
        }
        LocalDateTime desde = ZonaHorariaConsultorio.aHoraLocal(inicio);
        LocalDateTime hasta = ZonaHorariaConsultorio.aHoraLocal(fin);
        if (!desde.toLocalDate().equals(hasta.toLocalDate())) {
            return false;
        }
        boolean enHorario = franjasDelDia(horarios, desde.toLocalDate()).stream()
                .anyMatch(franja -> !desde.toLocalTime().isBefore(franja.inicio())
                        && !hasta.toLocalTime().isAfter(franja.fin()));
        return enHorario && bloqueos.stream()
                .noneMatch(b -> b.afectaAProfesional(profesionalId) && b.seSuperponeCon(inicio, fin));
    }

    /** El sillón está activo y no lo toca ningún bloqueo suyo ni del consultorio. */
    public static boolean sillonDisponible(Long sillonId, boolean activo, Collection<Bloqueo> bloqueos,
            Instant inicio, Instant fin) {
        validarRango(inicio, fin);
        return activo && bloqueos.stream()
                .noneMatch(b -> b.afectaASillon(sillonId) && b.seSuperponeCon(inicio, fin));
    }

    /** Franjas de atención de la fecha, ordenadas y con los rangos contiguos unidos. */
    public static List<FranjaLocal> franjasDelDia(Collection<HorarioAtencion> horarios, LocalDate fecha) {
        List<HorarioAtencion> delDia = horarios.stream()
                .filter(h -> h.getDia() == fecha.getDayOfWeek())
                .sorted(Comparator.comparing(HorarioAtencion::getInicio))
                .toList();
        List<FranjaLocal> franjas = new ArrayList<>();
        LocalTime inicio = null;
        LocalTime fin = null;
        for (HorarioAtencion horario : delDia) {
            if (inicio != null && !horario.getInicio().isAfter(fin)) {
                fin = horario.getFin().isAfter(fin) ? horario.getFin() : fin;
                continue;
            }
            if (inicio != null) {
                franjas.add(new FranjaLocal(fecha, inicio, fin));
            }
            inicio = horario.getInicio();
            fin = horario.getFin();
        }
        if (inicio != null) {
            franjas.add(new FranjaLocal(fecha, inicio, fin));
        }
        return franjas;
    }

    private static void validarRango(Instant inicio, Instant fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new ValidacionDeDominioException("El inicio del rango consultado debe ser anterior al fin");
        }
    }
}

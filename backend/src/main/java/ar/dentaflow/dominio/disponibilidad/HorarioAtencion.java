package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Objects;

/**
 * Rango semanal de atención de un profesional: día de la semana y horas de reloj del consultorio
 * (SU-09). Es una regla que se repite cada semana, no un instante, por eso no se convierte a UTC.
 * Precisión de minutos; no cruza la medianoche. Sin superposición por profesional y día (US-002).
 */
public class HorarioAtencion {

    private Long id;
    private Long profesionalId;
    private DayOfWeek dia;
    private LocalTime inicio;
    private LocalTime fin;

    protected HorarioAtencion() {
    }

    private HorarioAtencion(Long profesionalId, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        if (profesionalId == null) {
            throw new ValidacionDeDominioException("El horario debe pertenecer a un profesional existente");
        }
        validarRango(dia, inicio, fin);
        this.profesionalId = profesionalId;
        this.dia = dia;
        this.inicio = inicio;
        this.fin = fin;
    }

    /** Crea un rango para el profesional indicado por id. */
    public static HorarioAtencion nuevo(Long profesionalId, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        return new HorarioAtencion(profesionalId, dia, inicio, fin);
    }

    /** Cambia día y horas con las mismas validaciones del alta; si falla no cambia nada. */
    public void editar(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        validarRango(dia, inicio, fin);
        this.dia = dia;
        this.inicio = inicio;
        this.fin = fin;
    }

    /** Mismo profesional, mismo día y rangos {@code [inicio, fin)} que se cruzan; contiguos no. */
    public boolean seSuperponeCon(HorarioAtencion otro) {
        return profesionalId.equals(otro.profesionalId) && dia == otro.dia
                && inicio.isBefore(otro.fin) && otro.inicio.isBefore(fin);
    }

    /**
     * Falla con {@link ConflictoException} si {@code horario} se superpone con alguno de los
     * existentes. Ignora el rango con el mismo id (edición del propio rango).
     */
    public static void verificarSinSuperposicion(HorarioAtencion horario, Collection<HorarioAtencion> existentes) {
        for (HorarioAtencion existente : existentes) {
            boolean esElMismo = horario.id != null && horario.id.equals(existente.id);
            if (!esElMismo && horario.seSuperponeCon(existente)) {
                throw new ConflictoException("El rango se superpone con el horario existente del "
                        + NombresDeDia.de(existente.dia) + " de " + existente.inicio + " a " + existente.fin);
            }
        }
    }

    private static void validarRango(DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        if (dia == null) {
            throw new ValidacionDeDominioException("El día de la semana es obligatorio");
        }
        if (inicio == null) {
            throw new ValidacionDeDominioException("La hora de inicio es obligatoria");
        }
        if (fin == null) {
            throw new ValidacionDeDominioException("La hora de fin es obligatoria");
        }
        if (inicio.getSecond() != 0 || inicio.getNano() != 0 || fin.getSecond() != 0 || fin.getNano() != 0) {
            throw new ValidacionDeDominioException("Las horas del horario van en minutos exactos (HH:mm)");
        }
        if (!inicio.isBefore(fin)) {
            throw new ValidacionDeDominioException("La hora de inicio debe ser anterior a la hora de fin");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getProfesionalId() {
        return profesionalId;
    }

    public DayOfWeek getDia() {
        return dia;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public LocalTime getFin() {
        return fin;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof HorarioAtencion that && id != null && Objects.equals(id, that.getId());
    }

    @Override
    public int hashCode() {
        return HorarioAtencion.class.hashCode();
    }
}

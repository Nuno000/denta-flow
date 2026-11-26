package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, edición, baja y consulta del horario de atención (US-002, design D6). Para que dos altas
 * simultáneas no dejen rangos superpuestos, bloquea la fila del profesional ({@code FOR UPDATE})
 * antes de leer los rangos del día y guardar: las operaciones sobre el mismo profesional se
 * serializan y las de profesionales distintos no se esperan.
 */
@Service
@Transactional
public class ServicioDeHorarios {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeHorarios.class);

    private final HorarioAtencionRepositorio horarios;
    private final ProfesionalRepositorio profesionales;

    public ServicioDeHorarios(HorarioAtencionRepositorio horarios, ProfesionalRepositorio profesionales) {
        this.horarios = horarios;
        this.profesionales = profesionales;
    }

    /** Rangos del profesional por día (lunes primero) y hora; 404 si el profesional no existe. */
    @Transactional(readOnly = true)
    public List<HorarioAtencion> listarDelProfesional(Long profesionalId) {
        profesionales.buscarPorId(profesionalId).orElseThrow(() -> profesionalNoEncontrado(profesionalId));
        return horarios.listarDelProfesional(profesionalId);
    }

    @Transactional(readOnly = true)
    public HorarioAtencion obtener(Long id) {
        return horarios.buscarPorId(id).orElseThrow(() -> horarioNoEncontrado(id));
    }

    /** Agrega un rango a un profesional activo; 409 si se superpone con otro del mismo día. */
    public HorarioAtencion crear(Long profesionalId, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        Profesional profesional = profesionales.bloquearPorId(profesionalId)
                .orElseThrow(() -> profesionalNoEncontrado(profesionalId));
        if (!profesional.isActivo()) {
            throw new OperacionNoPermitidaException("No se pueden cargar horarios a un profesional inactivo");
        }
        HorarioAtencion nuevo = HorarioAtencion.nuevo(profesionalId, dia, inicio, fin);
        HorarioAtencion.verificarSinSuperposicion(nuevo, horarios.listarDelProfesionalYDia(profesionalId, dia));
        HorarioAtencion guardado = horarios.guardar(nuevo);
        LOG.info("Horario {} del profesional {} guardado", guardado.getId(), profesionalId);
        return guardado;
    }

    /**
     * Cambia día y horas del rango. Verifica contra los demás rangos del día nuevo antes de tocar
     * la entidad, así un conflicto no deja nada cambiado.
     */
    public HorarioAtencion editar(Long id, DayOfWeek dia, LocalTime inicio, LocalTime fin) {
        HorarioAtencion horario = obtener(id);
        Long profesionalId = horario.getProfesionalId();
        profesionales.bloquearPorId(profesionalId).orElseThrow(() -> profesionalNoEncontrado(profesionalId));
        HorarioAtencion candidato = HorarioAtencion.nuevo(profesionalId, dia, inicio, fin);
        List<HorarioAtencion> otros = horarios.listarDelProfesionalYDia(profesionalId, dia).stream()
                .filter(existente -> !Objects.equals(existente.getId(), id))
                .toList();
        HorarioAtencion.verificarSinSuperposicion(candidato, otros);
        horario.editar(dia, inicio, fin);
        HorarioAtencion guardado = horarios.guardar(horario);
        LOG.info("Horario {} del profesional {} guardado", id, profesionalId);
        return guardado;
    }

    public void eliminar(Long id) {
        HorarioAtencion horario = obtener(id);
        horarios.eliminar(horario);
        LOG.info("Horario {} del profesional {} eliminado", id, horario.getProfesionalId());
    }

    private static RecursoNoEncontradoException profesionalNoEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el profesional " + id);
    }

    private static RecursoNoEncontradoException horarioNoEncontrado(Long id) {
        return new RecursoNoEncontradoException("No existe el horario " + id);
    }
}

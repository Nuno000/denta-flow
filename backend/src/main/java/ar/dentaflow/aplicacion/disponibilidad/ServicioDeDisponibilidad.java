package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.CalendarioDeDisponibilidad;
import ar.dentaflow.dominio.disponibilidad.FranjaLocal;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link DisponibilidadDeRecursos}: lee recurso, horarios y bloqueos y delega la
 * decisión en {@link CalendarioDeDisponibilidad} (Java puro). Solo lectura; si el llamador tiene una
 * transacción abierta (C-09) participa de ella y lee el último estado confirmado.
 */
@Service
@Transactional(readOnly = true)
public class ServicioDeDisponibilidad implements DisponibilidadDeRecursos {

    private final ProfesionalRepositorio profesionales;
    private final SillonRepositorio sillones;
    private final HorarioAtencionRepositorio horarios;
    private final BloqueoRepositorio bloqueos;

    public ServicioDeDisponibilidad(ProfesionalRepositorio profesionales, SillonRepositorio sillones,
            HorarioAtencionRepositorio horarios, BloqueoRepositorio bloqueos) {
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.horarios = horarios;
        this.bloqueos = bloqueos;
    }

    @Override
    public boolean profesionalDisponible(Long profesionalId, Instant inicio, Instant fin) {
        validarRango(inicio, fin);
        Profesional profesional = profesional(profesionalId);
        DayOfWeek dia = ZonaHorariaConsultorio.aHoraLocal(inicio).getDayOfWeek();
        return CalendarioDeDisponibilidad.profesionalDisponible(profesionalId, profesional.isActivo(),
                horarios.listarDelProfesionalYDia(profesionalId, dia),
                bloqueos.listarQueAfectanAProfesional(profesionalId, inicio, fin), inicio, fin);
    }

    @Override
    public boolean sillonDisponible(Long sillonId, Instant inicio, Instant fin) {
        validarRango(inicio, fin);
        Sillon sillon = sillones.buscarPorId(sillonId)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el sillón " + sillonId));
        return CalendarioDeDisponibilidad.sillonDisponible(sillonId, sillon.isActivo(),
                bloqueos.listarQueAfectanASillon(sillonId, inicio, fin), inicio, fin);
    }

    @Override
    public List<FranjaLocal> franjasDeAtencion(Long profesionalId, LocalDate fecha) {
        profesional(profesionalId);
        return CalendarioDeDisponibilidad.franjasDelDia(
                horarios.listarDelProfesionalYDia(profesionalId, fecha.getDayOfWeek()), fecha);
    }

    @Override
    public List<Bloqueo> bloqueosQueAfectan(Long profesionalId, LocalDate fecha) {
        profesional(profesionalId);
        Instant desde = ZonaHorariaConsultorio.aInstante(fecha.atStartOfDay());
        Instant hasta = ZonaHorariaConsultorio.aInstante(fecha.plusDays(1).atStartOfDay());
        return bloqueos.listarQueAfectanAProfesional(profesionalId, desde, hasta);
    }

    private Profesional profesional(Long id) {
        return profesionales.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el profesional " + id));
    }

    private static void validarRango(Instant inicio, Instant fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new ValidacionDeDominioException("El inicio del rango consultado debe ser anterior al fin");
        }
    }
}

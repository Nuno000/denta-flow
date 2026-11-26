package ar.dentaflow.aplicacion.disponibilidad;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alta, edición, baja y consulta de bloqueos (US-002, design D7). Para los ámbitos profesional y
 * sillón bloquea la fila del recurso antes de guardar, con el mismo orden que la creación de turnos
 * (C-09), así la lista de turnos afectados es coherente. El aviso no resuelve PA-11: el bloqueo se
 * guarda y los turnos existentes no se tocan.
 */
@Service
@Transactional
public class ServicioDeBloqueos {

    /** Tope de la consulta por rango, para no devolver años de bloqueos por un error de tipeo. */
    public static final Duration RANGO_MAXIMO = Duration.ofDays(366);

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeBloqueos.class);

    private final BloqueoRepositorio bloqueos;
    private final ProfesionalRepositorio profesionales;
    private final SillonRepositorio sillones;
    private final TurnosAfectadosPorBloqueo turnosAfectados;
    private final Clock reloj;

    public ServicioDeBloqueos(BloqueoRepositorio bloqueos, ProfesionalRepositorio profesionales,
            SillonRepositorio sillones, TurnosAfectadosPorBloqueo turnosAfectados, Clock reloj) {
        this.bloqueos = bloqueos;
        this.profesionales = profesionales;
        this.sillones = sillones;
        this.turnosAfectados = turnosAfectados;
        this.reloj = reloj;
    }

    public ResultadoBloqueo crear(AmbitoBloqueo ambito, Long profesionalId, Long sillonId, Instant inicio,
            Instant fin, MotivoBloqueo motivo, String detalle, Long actor) {
        validarRecursoSegunAmbito(ambito, profesionalId, sillonId);
        bloquearRecursoActivo(ambito, profesionalId, sillonId);
        Instant ahora = Instant.now(reloj);
        Bloqueo nuevo = switch (ambito) {
            case PROFESIONAL -> Bloqueo.deProfesional(profesionalId, inicio, fin, motivo, detalle, actor, ahora);
            case SILLON -> Bloqueo.deSillon(sillonId, inicio, fin, motivo, detalle, actor, ahora);
            case CONSULTORIO -> Bloqueo.delConsultorio(inicio, fin, motivo, detalle, actor, ahora);
        };
        return guardarConAviso(nuevo, actor);
    }

    /**
     * Cambia franja, motivo y detalle. Ámbito y recurso no se editan: si vienen y son distintos de
     * los guardados es un dato inválido y no cambia nada.
     */
    public ResultadoBloqueo editar(Long id, Instant inicio, Instant fin, MotivoBloqueo motivo, String detalle,
            AmbitoBloqueo ambito, Long profesionalId, Long sillonId, Long actor) {
        Bloqueo bloqueo = obtener(id);
        if (ambito != null && ambito != bloqueo.getAmbito()
                || profesionalId != null && !Objects.equals(profesionalId, bloqueo.getProfesionalId())
                || sillonId != null && !Objects.equals(sillonId, bloqueo.getSillonId())) {
            throw new ValidacionDeDominioException(
                    "El ámbito y el recurso de un bloqueo no se editan: borralo y creá uno nuevo");
        }
        bloquearRecurso(bloqueo.getAmbito(), bloqueo.getProfesionalId(), bloqueo.getSillonId());
        bloqueo.reprogramar(inicio, fin, motivo, detalle, actor, Instant.now(reloj));
        return guardarConAviso(bloqueo, actor);
    }

    public void eliminar(Long id) {
        Bloqueo bloqueo = obtener(id);
        bloqueos.eliminar(bloqueo);
        LOG.info("Bloqueo {} eliminado", id);
    }

    @Transactional(readOnly = true)
    public Bloqueo obtener(Long id) {
        return bloqueos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el bloqueo " + id));
    }

    /** Bloqueos que se superponen con {@code [desde, hasta)}, con filtros opcionales por recurso. */
    @Transactional(readOnly = true)
    public List<Bloqueo> listar(Instant desde, Instant hasta, Long profesionalId, Long sillonId) {
        validarRangoDeConsulta(desde, hasta);
        return bloqueos.listarQueSeSuperponen(desde, hasta, profesionalId, sillonId);
    }

    /** Bloqueos del profesional y del consultorio en el rango: lo que ve un usuario con rol profesional. */
    @Transactional(readOnly = true)
    public List<Bloqueo> listarQueAfectanAProfesional(Long profesionalId, Instant desde, Instant hasta) {
        validarRangoDeConsulta(desde, hasta);
        return bloqueos.listarQueAfectanAProfesional(profesionalId, desde, hasta);
    }

    private ResultadoBloqueo guardarConAviso(Bloqueo bloqueo, Long actor) {
        Bloqueo guardado = bloqueos.guardar(bloqueo);
        LOG.info("Bloqueo {} ({}) guardado por el usuario {}", guardado.getId(), guardado.getAmbito(), actor);
        return new ResultadoBloqueo(guardado, turnosAfectados.listar(guardado));
    }

    private static void validarRecursoSegunAmbito(AmbitoBloqueo ambito, Long profesionalId, Long sillonId) {
        if (ambito == null) {
            throw new ValidacionDeDominioException("El ámbito del bloqueo es obligatorio");
        }
        boolean coherente = switch (ambito) {
            case PROFESIONAL -> profesionalId != null && sillonId == null;
            case SILLON -> sillonId != null && profesionalId == null;
            case CONSULTORIO -> profesionalId == null && sillonId == null;
        };
        if (!coherente) {
            throw new ValidacionDeDominioException("Un bloqueo de ámbito " + ambito
                    + " debe indicar " + switch (ambito) {
                        case PROFESIONAL -> "solo un profesional";
                        case SILLON -> "solo un sillón";
                        case CONSULTORIO -> "ningún recurso";
                    });
        }
    }

    private void bloquearRecursoActivo(AmbitoBloqueo ambito, Long profesionalId, Long sillonId) {
        if (ambito == AmbitoBloqueo.PROFESIONAL) {
            Profesional profesional = bloquearProfesional(profesionalId);
            if (!profesional.isActivo()) {
                throw new OperacionNoPermitidaException("No se puede bloquear un profesional inactivo");
            }
        } else if (ambito == AmbitoBloqueo.SILLON) {
            Sillon sillon = bloquearSillon(sillonId);
            if (!sillon.isActivo()) {
                throw new OperacionNoPermitidaException("No se puede bloquear un sillón inactivo");
            }
        }
    }

    private void bloquearRecurso(AmbitoBloqueo ambito, Long profesionalId, Long sillonId) {
        if (ambito == AmbitoBloqueo.PROFESIONAL) {
            bloquearProfesional(profesionalId);
        } else if (ambito == AmbitoBloqueo.SILLON) {
            bloquearSillon(sillonId);
        }
    }

    private Profesional bloquearProfesional(Long id) {
        return profesionales.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el profesional " + id));
    }

    private Sillon bloquearSillon(Long id) {
        return sillones.bloquearPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el sillón " + id));
    }

    private static void validarRangoDeConsulta(Instant desde, Instant hasta) {
        if (desde == null || hasta == null) {
            throw new ValidacionDeDominioException("La consulta de bloqueos necesita las fechas desde y hasta");
        }
        if (!desde.isBefore(hasta)) {
            throw new ValidacionDeDominioException("La fecha desde debe ser anterior a la fecha hasta");
        }
        if (Duration.between(desde, hasta).compareTo(RANGO_MAXIMO) > 0) {
            throw new ValidacionDeDominioException("El rango de consulta no puede superar los 366 días");
        }
    }
}

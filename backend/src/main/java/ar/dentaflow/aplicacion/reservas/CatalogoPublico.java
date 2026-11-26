package ar.dentaflow.aplicacion.reservas;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lo que el paciente puede elegir al reservar (C-16 design D3, spec "Catálogo público para reservar"): solo
 * prestaciones activas y profesionales activos que realizan la prestación. Solo lectura.
 */
@Service
public class CatalogoPublico {

    private final PrestacionRepositorio prestaciones;
    private final ProfesionalRepositorio profesionales;

    public CatalogoPublico(PrestacionRepositorio prestaciones, ProfesionalRepositorio profesionales) {
        this.prestaciones = prestaciones;
        this.profesionales = profesionales;
    }

    /** Prestaciones activas ordenadas por nombre. */
    @Transactional(readOnly = true)
    public List<Prestacion> prestaciones() {
        return prestaciones.listarActivas();
    }

    /** Profesionales activos que realizan la prestación; inexistente o inactiva es "no encontrada". */
    @Transactional(readOnly = true)
    public List<Profesional> profesionalesDe(Long prestacionId) {
        prestaciones.buscarPorId(prestacionId)
                .filter(Prestacion::isActiva)
                .orElseThrow(() -> new RecursoNoEncontradoException("La prestación no existe o no se puede reservar"));
        return profesionales.listarActivosQueRealizan(prestacionId);
    }
}

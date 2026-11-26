package ar.dentaflow.aplicacion.recursos;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de profesionales y de las prestaciones que realizan, por el administrador, y consulta por el
 * personal interno (US-001, spec gestion-recursos, design D1, D4 y D5). Sin borrado: la baja es la
 * desactivación, que conserva las prestaciones.
 */
@Service
@Transactional
public class ServicioDeProfesionales {

    static final String EMAIL_EN_USO = "Ya existe un profesional con ese email";
    static final String MATRICULA_EN_USO = "Ya existe un profesional con esa matrícula";

    private static final Map<String, String> CONSTRAINTS = Map.of(
            "uk_profesional_email", EMAIL_EN_USO,
            "uk_profesional_matricula", MATRICULA_EN_USO);

    private final ProfesionalRepositorio profesionales;
    private final PrestacionRepositorio prestaciones;

    public ServicioDeProfesionales(ProfesionalRepositorio profesionales, PrestacionRepositorio prestaciones) {
        this.profesionales = profesionales;
        this.prestaciones = prestaciones;
    }

    @Transactional(readOnly = true)
    public List<Profesional> listar() {
        return profesionales.listarTodos();
    }

    @Transactional(readOnly = true)
    public Profesional obtener(Long id) {
        return buscar(id);
    }

    public Profesional crear(String nombre, String apellido, String matricula, String email) {
        Profesional nuevo = Profesional.nuevo(nombre, apellido, matricula, email);
        verificarDatosLibres(null, nuevo);
        return guardar(nuevo);
    }

    /** Edita los datos personales validando antes de modificar; no toca el estado ni las prestaciones. */
    public Profesional actualizar(Long id, String nombre, String apellido, String matricula, String email) {
        Profesional profesional = buscar(id);
        Profesional candidato = Profesional.nuevo(nombre, apellido, matricula, email);
        verificarDatosLibres(id, candidato);
        profesional.actualizarDatos(candidato.getNombre(), candidato.getApellido(), candidato.getMatricula(),
                candidato.getEmail());
        return guardar(profesional);
    }

    public Profesional desactivar(Long id) {
        Profesional profesional = buscar(id);
        profesional.desactivar();
        return guardar(profesional);
    }

    public Profesional activar(Long id) {
        Profesional profesional = buscar(id);
        profesional.activar();
        return guardar(profesional);
    }

    /**
     * Reemplaza el conjunto de prestaciones (design D5). Valida todo antes de modificar: cada prestación debe
     * existir, y las que se agregan deben estar activas; las inactivas que ya tenía se conservan. Los ids
     * repetidos cuentan una sola vez.
     */
    public Profesional asignarPrestaciones(Long id, Collection<Long> prestacionIds) {
        Profesional profesional = buscar(id);
        Set<Long> pedidas = new LinkedHashSet<>(prestacionIds);
        List<Prestacion> agregadas = new ArrayList<>();
        for (Long prestacionId : pedidas) {
            Prestacion prestacion = prestaciones.buscarPorId(prestacionId)
                    .orElseThrow(() -> new ValidacionDeDominioException("La prestación " + prestacionId + " no existe"));
            if (!profesional.realiza(prestacionId)) {
                if (!prestacion.isActiva()) {
                    throw new ValidacionDeDominioException(
                            "No se puede asignar la prestación inactiva \"" + prestacion.getNombre() + "\"");
                }
                agregadas.add(prestacion);
            }
        }
        for (Long actual : profesional.prestacionIds()) {
            if (!pedidas.contains(actual)) {
                profesional.deshabilitarPrestacion(actual);
            }
        }
        agregadas.forEach(profesional::habilitarPrestacion);
        return guardar(profesional);
    }

    private void verificarDatosLibres(Long idPropio, Profesional candidato) {
        List<Profesional> todos = profesionales.listarTodos();
        Duplicados.verificar(todos, Profesional::getId, Profesional::getEmail, idPropio, candidato.getEmail(),
                EMAIL_EN_USO);
        Duplicados.verificar(todos, Profesional::getId, Profesional::getMatricula, idPropio, candidato.getMatricula(),
                MATRICULA_EN_USO);
    }

    private Profesional buscar(Long id) {
        return profesionales.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el profesional " + id));
    }

    private Profesional guardar(Profesional profesional) {
        return Duplicados.guardar(() -> profesionales.guardar(profesional), CONSTRAINTS);
    }
}

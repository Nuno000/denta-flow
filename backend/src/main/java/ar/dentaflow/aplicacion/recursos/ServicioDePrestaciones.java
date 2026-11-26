package ar.dentaflow.aplicacion.recursos;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de prestaciones y sus duraciones por el administrador y consulta por el personal interno (US-001,
 * RN-TU-03, spec gestion-recursos, design D1). Cambiar la duración no afecta a los turnos ya creados.
 */
@Service
@Transactional
public class ServicioDePrestaciones {

    static final String NOMBRE_EN_USO = "Ya existe una prestación con ese nombre";

    private final PrestacionRepositorio prestaciones;

    public ServicioDePrestaciones(PrestacionRepositorio prestaciones) {
        this.prestaciones = prestaciones;
    }

    @Transactional(readOnly = true)
    public List<Prestacion> listar() {
        return prestaciones.listarTodas();
    }

    @Transactional(readOnly = true)
    public Prestacion obtener(Long id) {
        return buscar(id);
    }

    public Prestacion crear(String nombre, Integer duracionMinutos) {
        Prestacion nueva = Prestacion.nueva(nombre, duracionMinutos);
        verificarNombreLibre(null, nueva.getNombre());
        return guardar(nueva);
    }

    /** Cambia nombre y duración validando antes de modificar, así un rechazo deja la prestación como estaba. */
    public Prestacion actualizar(Long id, String nombre, Integer duracionMinutos) {
        Prestacion prestacion = buscar(id);
        Prestacion candidata = Prestacion.nueva(nombre, duracionMinutos);
        verificarNombreLibre(id, candidata.getNombre());
        prestacion.actualizar(candidata.getNombre(), candidata.getDuracionMinutos());
        return guardar(prestacion);
    }

    public Prestacion desactivar(Long id) {
        Prestacion prestacion = buscar(id);
        prestacion.desactivar();
        return guardar(prestacion);
    }

    public Prestacion activar(Long id) {
        Prestacion prestacion = buscar(id);
        prestacion.activar();
        return guardar(prestacion);
    }

    private void verificarNombreLibre(Long idPropio, String nombre) {
        Duplicados.verificar(prestaciones.listarTodas(), Prestacion::getId, Prestacion::getNombre, idPropio, nombre,
                NOMBRE_EN_USO);
    }

    private Prestacion buscar(Long id) {
        return prestaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la prestación " + id));
    }

    private Prestacion guardar(Prestacion prestacion) {
        return Duplicados.guardar(() -> prestaciones.guardar(prestacion), Map.of("uk_prestacion_nombre", NOMBRE_EN_USO));
    }
}

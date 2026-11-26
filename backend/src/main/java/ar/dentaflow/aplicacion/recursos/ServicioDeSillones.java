package ar.dentaflow.aplicacion.recursos;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de sillones por el administrador y consulta por el personal interno (US-001, spec
 * gestion-recursos, design D1). Sin borrado: la baja es la desactivación.
 */
@Service
@Transactional
public class ServicioDeSillones {

    static final String NOMBRE_EN_USO = "Ya existe un sillón con ese nombre";

    private final SillonRepositorio sillones;

    public ServicioDeSillones(SillonRepositorio sillones) {
        this.sillones = sillones;
    }

    @Transactional(readOnly = true)
    public List<Sillon> listar() {
        return sillones.listarTodos();
    }

    @Transactional(readOnly = true)
    public Sillon obtener(Long id) {
        return buscar(id);
    }

    public Sillon crear(String nombre) {
        Sillon nuevo = Sillon.nuevo(nombre);
        verificarNombreLibre(null, nuevo.getNombre());
        return guardar(nuevo);
    }

    /** Renombra validando antes de modificar, así un rechazo deja el sillón como estaba. */
    public Sillon renombrar(Long id, String nombre) {
        Sillon sillon = buscar(id);
        String valido = Sillon.nuevo(nombre).getNombre();
        verificarNombreLibre(id, valido);
        sillon.renombrar(valido);
        return guardar(sillon);
    }

    public Sillon desactivar(Long id) {
        Sillon sillon = buscar(id);
        sillon.desactivar();
        return guardar(sillon);
    }

    public Sillon activar(Long id) {
        Sillon sillon = buscar(id);
        sillon.activar();
        return guardar(sillon);
    }

    private void verificarNombreLibre(Long idPropio, String nombre) {
        Duplicados.verificar(sillones.listarTodos(), Sillon::getId, Sillon::getNombre, idPropio, nombre, NOMBRE_EN_USO);
    }

    private Sillon buscar(Long id) {
        return sillones.buscarPorId(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe el sillón " + id));
    }

    private Sillon guardar(Sillon sillon) {
        return Duplicados.guardar(() -> sillones.guardar(sillon), Map.of("uk_sillon_nombre", NOMBRE_EN_USO));
    }
}

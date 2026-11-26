package ar.dentaflow.aplicacion.configuracion;

import ar.dentaflow.dominio.configuracion.ClaveParametro;
import ar.dentaflow.dominio.configuracion.Parametro;
import ar.dentaflow.dominio.configuracion.ParametroRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta y edición de los parámetros del consultorio por el administrador (design D6), y lectura
 * tipada para otros procesos. Sin caché: cada lectura va a la base, así un cambio se ve de inmediato.
 * Ediciones simultáneas: gana la última escritura, con la fila completa escrita de forma atómica.
 */
@Service
@Transactional
public class ServicioDeParametros implements ParametrosDelConsultorio {

    private static final Logger LOG = LoggerFactory.getLogger(ServicioDeParametros.class);

    private final ParametroRepositorio parametros;
    private final Clock reloj;

    public ServicioDeParametros(ParametroRepositorio parametros, Clock reloj) {
        this.parametros = parametros;
        this.reloj = reloj;
    }

    /** Las filas en el orden del catálogo, no el de la base. */
    @Transactional(readOnly = true)
    public List<Parametro> listar() {
        return parametros.listarTodos().stream()
                .sorted(Comparator.comparing(Parametro::clave))
                .toList();
    }

    @Transactional(readOnly = true)
    public Parametro obtener(String codigo) {
        return fila(ClaveParametro.desdeCodigo(codigo));
    }

    /** Valida y guarda el valor nuevo; si es inválido no cambia nada. */
    public Parametro actualizar(String codigo, String valor, Long actorId) {
        ClaveParametro clave = ClaveParametro.desdeCodigo(codigo);
        Parametro parametro = fila(clave);
        parametro.actualizar(valor, actorId, Instant.now(reloj));
        Parametro guardado = parametros.guardar(parametro);
        LOG.info("Parámetro {} actualizado por el usuario {}", clave.codigo(), actorId);
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Duration anticipacionMinima() {
        return Duration.ofHours(fila(ClaveParametro.ANTICIPACION_MINIMA_HORAS).valorEntero());
    }

    @Override
    @Transactional(readOnly = true)
    public AvisoPrivacidad avisoPrivacidad() {
        return new AvisoPrivacidad(fila(ClaveParametro.AVISO_PRIVACIDAD_TEXTO).valor(),
                fila(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA).valor());
    }

    @Override
    @Transactional(readOnly = true)
    public String contactoConsultorio() {
        return fila(ClaveParametro.CONTACTO_CONSULTORIO).valor();
    }

    private Parametro fila(ClaveParametro clave) {
        return parametros.buscar(clave)
                .orElseThrow(() -> new IllegalStateException("Falta el parámetro " + clave.codigo() + " en la base"));
    }
}

package ar.dentaflow.dominio.auditoria;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Un registro del historial de un turno (RN-AU-01): quién, cuándo, qué acción y qué campos cambiaron.
 * Es inmutable: no tiene setters y la persistencia no lo actualiza nunca (design D4). Los valores son
 * pares campo-valor de texto solo de los campos que cambiaron; los instantes van en UTC ISO-8601.
 */
public class CambioDeTurno {

    private Long id;
    private Long turnoId;
    private TipoDeActor actorTipo;
    private Long usuarioId;
    private Instant ocurridoEn;
    private AccionDeTurno accion;
    private Map<String, String> valorAnterior;
    private Map<String, String> valorNuevo;

    protected CambioDeTurno() {
    }

    private CambioDeTurno(Long turnoId, Actor actor, Instant ocurridoEn, AccionDeTurno accion,
            Map<String, String> valorAnterior, Map<String, String> valorNuevo) {
        this.turnoId = turnoId;
        this.actorTipo = actor.tipo();
        this.usuarioId = actor.usuarioId();
        this.ocurridoEn = ocurridoEn;
        this.accion = accion;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
    }

    /**
     * Arma un registro nuevo (sin id). El valor anterior puede quedar vacío solo en la creación y al menos
     * uno de los dos valores debe tener contenido. Copia los mapas: cambiar el original después no lo afecta.
     */
    public static CambioDeTurno registrar(Long turnoId, Actor actor, Instant ocurridoEn, AccionDeTurno accion,
            Map<String, String> valorAnterior, Map<String, String> valorNuevo) {
        if (turnoId == null || turnoId <= 0) {
            throw new ValidacionDeDominioException("El turno auditado necesita un id válido");
        }
        if (actor == null) {
            throw new ValidacionDeDominioException("El actor del cambio es obligatorio");
        }
        if (ocurridoEn == null) {
            throw new ValidacionDeDominioException("La fecha y hora del cambio es obligatoria");
        }
        if (accion == null) {
            throw new ValidacionDeDominioException("La acción del cambio es obligatoria");
        }
        Map<String, String> anterior = copiaValidada(valorAnterior, "anterior");
        Map<String, String> nuevo = copiaValidada(valorNuevo, "nuevo");
        if (anterior.isEmpty() && nuevo.isEmpty()) {
            throw new ValidacionDeDominioException("El cambio necesita al menos un valor anterior o nuevo");
        }
        if (anterior.isEmpty() && accion != AccionDeTurno.CREACION) {
            throw new ValidacionDeDominioException(
                    "El valor anterior solo puede quedar vacío en la creación del turno");
        }
        return new CambioDeTurno(turnoId, actor, ocurridoEn, accion, anterior, nuevo);
    }

    private static Map<String, String> copiaValidada(Map<String, String> valores, String cual) {
        if (valores == null || valores.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copia = new LinkedHashMap<>();
        valores.forEach((campo, valor) -> {
            if (campo == null || campo.isBlank()) {
                throw new ValidacionDeDominioException("Hay un campo sin nombre en el valor " + cual);
            }
            if (valor == null) {
                throw new ValidacionDeDominioException("El campo " + campo + " del valor " + cual + " no tiene valor");
            }
            copia.put(campo, valor);
        });
        return Collections.unmodifiableMap(copia);
    }

    public Long id() {
        return id;
    }

    public Long turnoId() {
        return turnoId;
    }

    public Actor actor() {
        return new Actor(actorTipo, usuarioId);
    }

    public Instant ocurridoEn() {
        return ocurridoEn;
    }

    public AccionDeTurno accion() {
        return accion;
    }

    /** Nunca nulo; vacío solo en la creación. No se puede modificar. */
    public Map<String, String> valorAnterior() {
        return valorAnterior == null ? Map.of() : Collections.unmodifiableMap(valorAnterior);
    }

    /** Nunca nulo. No se puede modificar. */
    public Map<String, String> valorNuevo() {
        return valorNuevo == null ? Map.of() : Collections.unmodifiableMap(valorNuevo);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof CambioDeTurno cambio)) {
            return false;
        }
        return id != null && id.equals(cambio.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getClass());
    }
}

package ar.dentaflow.dominio.auditoria;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/**
 * Catálogo cerrado de acciones auditables sobre un turno (spec "Acción de un catálogo cerrado"). La base
 * tiene el mismo catálogo en {@code ck_turno_historial_accion}: una acción nueva necesita las dos cosas.
 * El sobreturno no es una acción: es una {@link #CREACION} con {@code es_sobreturno} en el valor nuevo.
 */
public enum AccionDeTurno {
    CREACION("creacion"),
    CONFIRMACION("confirmacion"),
    CANCELACION("cancelacion"),
    REPROGRAMACION("reprogramacion"),
    CAMBIO_ESTADO("cambio_estado");

    private final String codigo;

    AccionDeTurno(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static AccionDeTurno desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(accion -> accion.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Acción de turno desconocida: " + codigo));
    }
}

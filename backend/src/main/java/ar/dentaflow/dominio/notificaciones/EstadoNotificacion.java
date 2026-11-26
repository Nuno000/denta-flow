package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/**
 * Estado del envío de una notificación. El código es el que se guarda en la base ({@code V014_1}, design D2).
 */
public enum EstadoNotificacion {
    /** Registrada: el envío está en marcha o se interrumpió. */
    EN_CURSO("en_curso"),
    /** El servidor de correo aceptó el mensaje. */
    ENVIADA("enviada"),
    /** Se agotaron los intentos o la falla es permanente. */
    FALLIDA("fallida"),
    /** No corresponde enviarla (por ejemplo, paciente anonimizado). */
    OMITIDA("omitida");

    private final String codigo;

    EstadoNotificacion(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static EstadoNotificacion desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(valor -> valor.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Estado de notificación desconocido: " + codigo));
    }
}

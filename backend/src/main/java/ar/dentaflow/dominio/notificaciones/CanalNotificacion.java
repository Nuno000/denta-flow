package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/**
 * Por dónde sale la notificación. En el MVP solo email (RN-NO-04 deja WhatsApp como enlace manual). El código
 * es el que se guarda en la base ({@code V014_1}, design D2).
 */
public enum CanalNotificacion {
    /** Email por SMTP. */
    EMAIL("email");

    private final String codigo;

    CanalNotificacion(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static CanalNotificacion desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(valor -> valor.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Canal de notificación desconocido: " + codigo));
    }
}

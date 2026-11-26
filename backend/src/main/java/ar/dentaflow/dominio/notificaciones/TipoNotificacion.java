package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/**
 * Qué email transaccional es. Los reintentos no son un tipo: se cuentan en {@code intentos}. El código es el que se
 * guarda en la base ({@code V014_1}, design D2).
 */
public enum TipoNotificacion {
    /** Confirmación de reserva con los enlaces del turno (C-14). */
    CONFIRMACION("confirmacion"),
    /** Recordatorio previo al turno (C-18). */
    RECORDATORIO("recordatorio");

    private final String codigo;

    TipoNotificacion(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static TipoNotificacion desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(valor -> valor.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Tipo de notificación desconocido: " + codigo));
    }
}

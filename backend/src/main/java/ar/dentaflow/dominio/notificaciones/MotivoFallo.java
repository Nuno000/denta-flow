package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/**
 * Categoría de una falla de envío. Nunca se guarda el texto del proveedor, que puede repetir la dirección. El
 * código es el que se guarda en la base ({@code V014_1}, design D2).
 */
public enum MotivoFallo {
    /** Servidor inalcanzable, tiempo de espera o rechazo temporal: se reintenta. */
    CONEXION("conexion"),
    /** El servidor rechazó las credenciales SMTP: no se reintenta. */
    AUTENTICACION("autenticacion"),
    /** El servidor rechazó la dirección del destinatario: no se reintenta. */
    DESTINATARIO("destinatario"),
    /** Otra falla: se trata como transitoria. */
    DESCONOCIDO("desconocido");

    private final String codigo;

    MotivoFallo(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static MotivoFallo desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(valor -> valor.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Motivo de falla desconocido: " + codigo));
    }
}

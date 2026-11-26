package ar.dentaflow.dominio.notificaciones;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Enlace {@code https://wa.me/<dígitos>?text=<mensaje>} (C-21, design D4). El texto se codifica en UTF-8 como
 * componente de URL con los espacios como {@code %20}: WhatsApp no siempre interpreta {@code +} como espacio.
 */
public final class EnlaceWhatsapp {

    private EnlaceWhatsapp() {
    }

    /** {@code telefono} ya normalizado por {@link TelefonoWhatsapp}. */
    public static String de(String telefono, String mensaje) {
        String texto = URLEncoder.encode(mensaje, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + telefono + "?text=" + texto;
    }
}

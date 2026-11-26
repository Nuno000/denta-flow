package ar.dentaflow.dominio.notificaciones;

import java.time.Instant;

/**
 * Valores que reemplazan los marcadores de la plantilla de WhatsApp (C-21, design D3). {@code profesional} es
 * nombre y apellido; {@code enlace} es el enlace de confirmación del turno (C-14).
 */
public record DatosDelMensajeWhatsapp(String nombre, Instant inicio, String prestacion, String profesional,
        String enlace) {
}

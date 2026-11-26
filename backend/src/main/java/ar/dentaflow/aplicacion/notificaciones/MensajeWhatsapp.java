package ar.dentaflow.aplicacion.notificaciones;

/**
 * Mensaje de WhatsApp prellenado de un turno (C-21, design D1). {@code enlace} es {@code null} si el paciente no
 * tiene un teléfono argentino utilizable; el mensaje viene armado igual.
 */
public record MensajeWhatsapp(String mensaje, String enlace) {
}

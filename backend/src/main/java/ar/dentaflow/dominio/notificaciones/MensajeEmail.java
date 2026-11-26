package ar.dentaflow.dominio.notificaciones;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;

/**
 * Email listo para enviar: destinatario, asunto, cuerpo en texto plano y en HTML ya escapado. {@link #toString()}
 * no muestra destinatario ni cuerpo (llevan datos personales y los enlaces con el token).
 */
public record MensajeEmail(String destinatario, String asunto, String textoPlano, String html) {

    public MensajeEmail {
        if (destinatario == null || destinatario.isBlank()) {
            throw new ValidacionDeDominioException("El destinatario del email es obligatorio");
        }
        if (asunto == null || textoPlano == null || html == null) {
            throw new ValidacionDeDominioException("El asunto y el cuerpo del email son obligatorios");
        }
    }

    @Override
    public String toString() {
        return "MensajeEmail[destinatario=***, asunto=***, cuerpo=***]";
    }
}

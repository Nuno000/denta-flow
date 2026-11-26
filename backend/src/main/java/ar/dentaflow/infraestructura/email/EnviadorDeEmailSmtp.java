package ar.dentaflow.infraestructura.email;

import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import ar.dentaflow.dominio.notificaciones.FallaDeEnvio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

/**
 * Canal SMTP con Spring Mail (design D1 y D4): Brevo, o Gmail con contraseña de aplicación, según
 * {@code SPRING_MAIL_*}. Envía texto plano y HTML en UTF-8. Traduce cada falla a una categoría
 * ({@link MotivoFallo}) y descarta el texto del proveedor, que puede repetir la dirección del destinatario.
 */
public class EnviadorDeEmailSmtp implements EnviadorDeEmail {

    private static final int PROFUNDIDAD_MAXIMA = 20;

    private final JavaMailSender sender;
    private final PropiedadesEmail propiedades;

    public EnviadorDeEmailSmtp(JavaMailSender sender, PropiedadesEmail propiedades) {
        this.sender = sender;
        this.propiedades = propiedades;
    }

    @Override
    public void enviar(MensajeEmail mensaje) {
        MimeMessage mime = armar(mensaje);
        try {
            sender.send(mime);
        } catch (MailException e) {
            throw new FallaDeEnvio(clasificar(e));
        }
    }

    private MimeMessage armar(MensajeEmail mensaje) {
        MimeMessage mime = sender.createMimeMessage();
        try {
            MimeMessageHelper ayudante = new MimeMessageHelper(mime, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            ayudante.setFrom(propiedades.from(), propiedades.nombreRemitente());
            ayudante.setTo(mensaje.destinatario());
            ayudante.setSubject(mensaje.asunto());
            ayudante.setText(mensaje.textoPlano(), mensaje.html());
            return mime;
        } catch (AddressException e) {
            throw new FallaDeEnvio(MotivoFallo.DESTINATARIO);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new FallaDeEnvio(MotivoFallo.DESCONOCIDO);
        }
    }

    static MotivoFallo clasificar(MailException error) {
        if (error instanceof MailAuthenticationException) {
            return MotivoFallo.AUTENTICACION;
        }
        List<Throwable> origenes = new ArrayList<>();
        origenes.add(error);
        if (error instanceof MailSendException envio) {
            origenes.addAll(envio.getFailedMessages().values());
        }
        boolean conexion = false;
        for (Throwable origen : origenes) {
            Throwable actual = origen;
            for (int i = 0; actual != null && i < PROFUNDIDAD_MAXIMA; i++, actual = actual.getCause()) {
                if (actual instanceof SendFailedException rechazo && rechazo.getInvalidAddresses() != null
                        && rechazo.getInvalidAddresses().length > 0) {
                    return MotivoFallo.DESTINATARIO;
                }
                if (actual instanceof IOException) {
                    conexion = true;
                }
            }
        }
        return conexion ? MotivoFallo.CONEXION : MotivoFallo.DESCONOCIDO;
    }
}

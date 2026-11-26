package ar.dentaflow.infraestructura.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.notificaciones.FallaDeEnvio;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Spec notificaciones-email — "Reintento ante fallas transitorias" (clasificación de la falla), "Estado del envío
 * consultable" (motivo sin datos del destinatario) y "Canal desacoplado del proveedor" (design D4).
 */
class EnviadorDeEmailSmtpTest {

    private static final String DESTINATARIO = "paciente@example.com";

    private final JavaMailSender sender = mock(JavaMailSender.class);
    private final EnviadorDeEmailSmtp enviador =
            new EnviadorDeEmailSmtp(sender, new PropiedadesEmail(ModoEnvio.SMTP, "turnos@consultorio.example",
                    "Consultorio Demo"));

    private final MensajeEmail mensaje = new MensajeEmail(DESTINATARIO, "Tu turno", "texto plano", "<p>html</p>");

    @BeforeEach
    void crearMensajesReales() {
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void enviaUnMensajeMultiparteUtf8ConElRemitenteConfigurado() throws Exception {
        enviador.enviar(mensaje);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());
        MimeMessage enviado = captor.getValue();
        enviado.saveChanges();
        assertThat(((InternetAddress) enviado.getFrom()[0]).getAddress()).isEqualTo("turnos@consultorio.example");
        assertThat(((InternetAddress) enviado.getFrom()[0]).getPersonal()).isEqualTo("Consultorio Demo");
        assertThat(enviado.getAllRecipients()).extracting(Address::toString).containsExactly(DESTINATARIO);
        assertThat(enviado.getSubject()).isEqualTo("Tu turno");
        assertThat(enviado.getContent()).isInstanceOf(MimeMultipart.class);
        assertThat(enviado.getContentType()).contains("multipart");
    }

    @Test
    void autenticacionRechazadaEsPermanente() {
        doThrow(new MailAuthenticationException("535 Authentication failed")).when(sender).send(any(MimeMessage.class));

        assertMotivo(MotivoFallo.AUTENTICACION);
    }

    @Test
    void direccionRechazadaEsDeDestinatarioYNoRepiteLaDireccion() throws Exception {
        SendFailedException rechazo = new SendFailedException("550 " + DESTINATARIO + " no existe", null,
                new Address[0], new Address[0], new Address[] {new InternetAddress(DESTINATARIO)});
        doThrow(new MailSendException(Map.<Object, Exception>of(new Object(), rechazo))).when(sender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> enviador.enviar(mensaje))
                .isInstanceOfSatisfying(FallaDeEnvio.class, falla -> {
                    assertThat(falla.motivo()).isEqualTo(MotivoFallo.DESTINATARIO);
                    assertThat(falla.getMessage()).doesNotContain(DESTINATARIO);
                    assertThat(falla.getCause()).isNull();
                });
    }

    @Test
    void servidorInalcanzableEsDeConexion() {
        doThrow(new MailSendException("Mail server connection failed",
                new MessagingException("Couldn't connect to host", new ConnectException("Connection refused"))))
                .when(sender).send(any(MimeMessage.class));

        assertMotivo(MotivoFallo.CONEXION);
    }

    @Test
    void tiempoDeEsperaEsDeConexion() {
        MessagingException timeout = new MessagingException("Exception reading response",
                new SocketTimeoutException("Read timed out"));
        doThrow(new MailSendException(Map.<Object, Exception>of(new Object(), timeout))).when(sender).send(any(MimeMessage.class));

        assertMotivo(MotivoFallo.CONEXION);
    }

    @Test
    void otraFallaEsDesconocida() {
        doThrow(new MailSendException("algo raro")).when(sender).send(any(MimeMessage.class));

        assertMotivo(MotivoFallo.DESCONOCIDO);
    }

    @Test
    void unErrorInesperadoDelServidorTambienSeClasifica() {
        doThrow(new MailException("inesperado") {
        }).when(sender).send(any(MimeMessage.class));

        assertMotivo(MotivoFallo.DESCONOCIDO);
    }

    private void assertMotivo(MotivoFallo esperado) {
        assertThatThrownBy(() -> enviador.enviar(mensaje))
                .isInstanceOfSatisfying(FallaDeEnvio.class, falla -> assertThat(falla.motivo()).isEqualTo(esperado));
    }
}

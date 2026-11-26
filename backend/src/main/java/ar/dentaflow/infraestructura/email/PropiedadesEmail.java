package ar.dentaflow.infraestructura.email;

import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del email ({@code app.mail}, design D7). El servidor SMTP y sus credenciales son los
 * {@code SPRING_MAIL_*} estándar de Spring Boot.
 *
 * @param modo            {@code smtp} o {@code simulado} ({@code APP_MAIL_MODO}; por defecto {@code simulado})
 * @param from            remitente ({@code APP_MAIL_FROM}); debe ser una dirección válida o no arranca
 * @param nombreRemitente nombre visible del remitente
 */
@ConfigurationProperties("app.mail")
public record PropiedadesEmail(
        @DefaultValue("simulado") ModoEnvio modo,
        @DefaultValue("turnos@dentaflow.local") String from,
        @DefaultValue("DentaFlow") String nombreRemitente) {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public PropiedadesEmail {
        if (from == null || !EMAIL.matcher(from).matches()) {
            throw new IllegalStateException("APP_MAIL_FROM debe ser una dirección de email válida "
                    + "(por ejemplo turnos@consultorio.example)");
        }
    }
}

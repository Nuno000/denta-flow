package ar.dentaflow.aplicacion.notificaciones;

import java.net.URI;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * URL pública del frontend con la que se arman los enlaces del paciente ({@code app.enlaces.base-url}, que toma
 * {@code APP_BASE_URL}; design D6 y D7). Debe ser absoluta {@code http} o {@code https}: si no, la aplicación no
 * arranca. Se guarda sin barras finales.
 */
@ConfigurationProperties("app.enlaces")
public record PropiedadesEnlaces(String baseUrl) {

    public PropiedadesEnlaces {
        baseUrl = validar(baseUrl);
    }

    private static String validar(String valor) {
        String error = "APP_BASE_URL debe ser una URL absoluta http o https (por ejemplo https://turnos.example)";
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("Falta APP_BASE_URL. " + error);
        }
        String sinBarras = valor.strip().replaceAll("/+$", "");
        URI uri;
        try {
            uri = URI.create(sinBarras);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(error);
        }
        String esquema = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!(esquema.equals("http") || esquema.equals("https")) || uri.getHost() == null) {
            throw new IllegalStateException(error);
        }
        return sinBarras;
    }
}

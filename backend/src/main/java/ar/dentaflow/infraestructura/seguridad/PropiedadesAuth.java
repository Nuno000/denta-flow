package ar.dentaflow.infraestructura.seguridad;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la autenticación del personal (C-04, design D2 y D7). El secreto llega siempre de
 * {@code AUTH_SECRET}: la aplicación no arranca sin una clave de al menos 32 bytes (mínimo de HS256).
 *
 * @param secreto         clave de firma de los tokens ({@code app.auth.secreto})
 * @param duracionToken   vigencia del token desde que se emite
 * @param emisor          valor del claim {@code iss}
 * @param intentosMaximos fallos de ingreso permitidos por email dentro de la ventana
 * @param ventanaIntentos ventana fija que empieza con el primer fallo
 */
@ConfigurationProperties("app.auth")
public record PropiedadesAuth(
        String secreto,
        @DefaultValue("PT8H") Duration duracionToken,
        @DefaultValue("dentaflow") String emisor,
        @DefaultValue("5") int intentosMaximos,
        @DefaultValue("PT15M") Duration ventanaIntentos) {

    public static final int BYTES_MINIMOS_SECRETO = 32;

    public PropiedadesAuth {
        if (secreto == null || secreto.isBlank()) {
            throw new IllegalStateException("Falta la clave de firma de tokens: definí AUTH_SECRET con al menos "
                    + BYTES_MINIMOS_SECRETO + " bytes aleatorios (por ejemplo, la salida de `openssl rand -base64 48`)");
        }
        if (secreto.getBytes(StandardCharsets.UTF_8).length < BYTES_MINIMOS_SECRETO) {
            throw new IllegalStateException("AUTH_SECRET es demasiado corta: necesita al menos "
                    + BYTES_MINIMOS_SECRETO + " bytes (generala con `openssl rand -base64 48`)");
        }
    }

    /** No muestra el secreto si el objeto termina en un log o en un mensaje de error. */
    @Override
    public String toString() {
        return "PropiedadesAuth[secreto=***, duracionToken=" + duracionToken + ", emisor=" + emisor
                + ", intentosMaximos=" + intentosMaximos + ", ventanaIntentos=" + ventanaIntentos + "]";
    }
}

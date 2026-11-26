package ar.dentaflow.aplicacion.notificaciones;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cómo se ejecuta el envío ({@code app.notificaciones}, design D5).
 *
 * @param asincronico          {@code true} (por defecto): el envío corre en un hilo propio y la request no espera
 *                             al servidor de correo. {@code false} (perfil {@code test}): quien llama espera a que
 *                             termine, aunque el envío igual corre en otro hilo y fuera de su transacción.
 * @param esperaEntreIntentos  espera antes del segundo intento; se duplica en el siguiente (1 s y 2 s por defecto)
 */
@ConfigurationProperties("app.notificaciones")
public record PropiedadesEnvio(
        @DefaultValue("true") boolean asincronico,
        @DefaultValue("PT1S") Duration esperaEntreIntentos) {

    public PropiedadesEnvio {
        if (esperaEntreIntentos == null || esperaEntreIntentos.isNegative()) {
            throw new IllegalStateException("app.notificaciones.espera-entre-intentos no puede ser negativa");
        }
    }
}

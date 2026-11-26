package ar.dentaflow.infraestructura.turnos;

import ar.dentaflow.dominio.turnos.GeneradorDeTokens;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Token de acceso de 256 bits (32 bytes de {@link SecureRandom}) en Base64 URL sin relleno: 43 caracteres
 * {@code [A-Za-z0-9_-]} (design D9, Q1). Un único {@code SecureRandom} por instancia (es seguro entre hilos).
 */
@Component
public class GeneradorDeTokensSeguro implements GeneradorDeTokens {

    private static final int BYTES = 32;

    private final SecureRandom aleatorio = new SecureRandom();

    @Override
    public String nuevoToken() {
        byte[] bytes = new byte[BYTES];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

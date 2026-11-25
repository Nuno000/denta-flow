package ar.dentaflow.infraestructura.seguridad;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Firma y validación de los tokens de acceso con HMAC-SHA256 y la clave de {@code AUTH_SECRET}
 * (design D1 y D2). El decodificador exige firma válida, {@code iss} propio y {@code exp} vigente según
 * el {@link Clock} de la aplicación, sin tolerancia extra.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesAuth.class)
public class ConfiguracionTokens {

    @Bean
    SecretKey claveDeFirma(PropiedadesAuth propiedades) {
        return new SecretKeySpec(propiedades.secreto().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey claveDeFirma) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveDeFirma));
    }

    @Bean
    JwtDecoder decodificadorJwt(SecretKey claveDeFirma, PropiedadesAuth propiedades, Clock reloj) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveDeFirma)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator vigencia = new JwtTimestampValidator(Duration.ZERO);
        vigencia.setClock(reloj);
        decodificador.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(vigencia, new JwtIssuerValidator(propiedades.emisor())));
        return decodificador;
    }

    @Bean
    EmisorDeTokensJwt emisorDeTokens(JwtEncoder codificadorJwt, PropiedadesAuth propiedades, Clock reloj) {
        return new EmisorDeTokensJwt(codificadorJwt, propiedades, reloj);
    }
}

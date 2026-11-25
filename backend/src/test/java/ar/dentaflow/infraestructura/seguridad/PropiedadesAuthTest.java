package ar.dentaflow.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Spec autenticacion-personal — "Clave de firma obligatoria" (design D2). */
class PropiedadesAuthTest {

    private static final String CLAVE_32_BYTES = "0123456789abcdef0123456789abcdef";

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PropiedadesAuth.class)
    static class Config {
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void sinSecretoNoArrancaYExplicaComoGenerarlo() {
        contexto.run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).rootCause()
                    .hasMessageContaining("AUTH_SECRET")
                    .hasMessageContaining("openssl rand -base64 48");
        });
    }

    @Test
    void conSecretoDe31BytesNoArranca() {
        contexto.withPropertyValues("app.auth.secreto=" + CLAVE_32_BYTES.substring(1))
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("32 bytes");
                });
    }

    @Test
    void conSecretoDe32BytesArrancaConLosValoresPorDefecto() {
        contexto.withPropertyValues("app.auth.secreto=" + CLAVE_32_BYTES)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    PropiedadesAuth propiedades = ctx.getBean(PropiedadesAuth.class);
                    assertThat(propiedades.duracionToken()).isEqualTo(Duration.ofHours(8));
                    assertThat(propiedades.emisor()).isEqualTo("dentaflow");
                    assertThat(propiedades.intentosMaximos()).isEqualTo(5);
                    assertThat(propiedades.ventanaIntentos()).isEqualTo(Duration.ofMinutes(15));
                });
    }

    @Test
    void losValoresSeConfiguran() {
        contexto.withPropertyValues("app.auth.secreto=" + CLAVE_32_BYTES, "app.auth.duracion-token=PT1H",
                        "app.auth.intentos-maximos=3", "app.auth.ventana-intentos=PT5M")
                .run(ctx -> {
                    PropiedadesAuth propiedades = ctx.getBean(PropiedadesAuth.class);
                    assertThat(propiedades.duracionToken()).isEqualTo(Duration.ofHours(1));
                    assertThat(propiedades.intentosMaximos()).isEqualTo(3);
                    assertThat(propiedades.ventanaIntentos()).isEqualTo(Duration.ofMinutes(5));
                });
    }
}

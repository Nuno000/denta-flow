package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Spec configuracion-entorno — "Configuración de email inválida detiene el arranque" (URL pública). */
class PropiedadesEnlacesTest {

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PropiedadesEnlaces.class)
    static class Config {
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Config.class);

    @Test
    void unaUrlRelativaNoArranca() {
        contexto.withPropertyValues("app.enlaces.base-url=turnos.example").run(ctx -> {
            assertThat(ctx).hasFailed();
            assertThat(ctx.getStartupFailure()).rootCause()
                    .hasMessageContaining("APP_BASE_URL")
                    .hasMessageContaining("absoluta");
        });
    }

    @Test
    void otroEsquemaNoArranca() {
        contexto.withPropertyValues("app.enlaces.base-url=ftp://turnos.example")
                .run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void sinValorNoArranca() {
        contexto.run(ctx -> assertThat(ctx).hasFailed());
    }

    @Test
    void quitaLaBarraFinal() {
        contexto.withPropertyValues("app.enlaces.base-url=https://turnos.example/").run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PropiedadesEnlaces.class).baseUrl()).isEqualTo("https://turnos.example");
        });
    }

    @Test
    void conservaUnaRutaBase() {
        contexto.withPropertyValues("app.enlaces.base-url=http://localhost:5173/app//")
                .run(ctx -> assertThat(ctx.getBean(PropiedadesEnlaces.class).baseUrl())
                        .isEqualTo("http://localhost:5173/app"));
    }
}

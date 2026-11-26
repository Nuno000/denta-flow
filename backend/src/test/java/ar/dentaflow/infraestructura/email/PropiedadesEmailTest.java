package ar.dentaflow.infraestructura.email;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Spec configuracion-entorno — "Configuración del email por variables de entorno" y "Configuración de email
 * inválida detiene el arranque" (design D7).
 */
class PropiedadesEmailTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
            .withUserConfiguration(ConfiguracionEmail.class);

    @Test
    void sinModoUsaElCanalSimulado() {
        contexto.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx.getBean(PropiedadesEmail.class).modo()).isEqualTo(ModoEnvio.SIMULADO);
            assertThat(ctx.getBean(EnviadorDeEmail.class)).isInstanceOf(EnviadorDeEmailSimulado.class);
        });
    }

    @Test
    void enModoSmtpSinServidorNoArrancaYNombraLaVariableSinMostrarLaContrasena() {
        contexto.withPropertyValues("app.mail.modo=smtp", "spring.mail.host=", "spring.mail.password=claveSecreta123")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause()
                            .hasMessageContaining("SPRING_MAIL_HOST")
                            .hasMessageNotContaining("claveSecreta123");
                });
    }

    @Test
    void enModoSmtpConServidorUsaElAdaptadorSmtp() {
        contexto.withPropertyValues("app.mail.modo=smtp", "spring.mail.host=smtp.example.com",
                        "app.mail.from=turnos@consultorio.example")
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx.getBean(EnviadorDeEmail.class)).isInstanceOf(EnviadorDeEmailSmtp.class);
                });
    }

    @Test
    void unRemitenteInvalidoNoArranca() {
        contexto.withPropertyValues("app.mail.from=no-es-un-email")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    assertThat(ctx.getStartupFailure()).rootCause().hasMessageContaining("APP_MAIL_FROM");
                });
    }

    @Test
    void elRemitentePorDefectoEsValido() {
        contexto.run(ctx -> assertThat(ctx.getBean(PropiedadesEmail.class).from()).contains("@"));
    }
}

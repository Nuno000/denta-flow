package ar.dentaflow.infraestructura.email;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * Elige el canal de salida de los emails según {@code APP_MAIL_MODO} (design D4 y D7): el simulado por defecto, o
 * el SMTP, que exige {@code SPRING_MAIL_HOST} para arrancar. Los mensajes de error nombran la variable faltante y
 * nunca muestran credenciales.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PropiedadesEmail.class)
public class ConfiguracionEmail {

    @Bean
    @ConditionalOnProperty(name = "app.mail.modo", havingValue = "simulado", matchIfMissing = true)
    EnviadorDeEmailSimulado enviadorDeEmailSimulado() {
        return new EnviadorDeEmailSimulado();
    }

    @Bean
    @ConditionalOnProperty(name = "app.mail.modo", havingValue = "smtp")
    EnviadorDeEmailSmtp enviadorDeEmailSmtp(ObjectProvider<JavaMailSender> sender, PropiedadesEmail propiedades,
            Environment entorno) {
        String host = entorno.getProperty("spring.mail.host");
        JavaMailSender disponible = sender.getIfAvailable();
        if (host == null || host.isBlank() || disponible == null) {
            throw new IllegalStateException("Con APP_MAIL_MODO=smtp hace falta el servidor de correo: definí "
                    + "SPRING_MAIL_HOST (y SPRING_MAIL_PORT, SPRING_MAIL_USERNAME y SPRING_MAIL_PASSWORD)");
        }
        return new EnviadorDeEmailSmtp(disponible, propiedades);
    }
}

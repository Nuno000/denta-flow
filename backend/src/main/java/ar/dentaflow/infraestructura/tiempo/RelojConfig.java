package ar.dentaflow.infraestructura.tiempo;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Única fuente de la hora actual. Todo lo que necesite "ahora" inyecta este {@link Clock}; las
 * pruebas lo reemplazan por {@code Clock.fixed(...)} con un bean {@code @Primary}.
 */
@Configuration(proxyBeanMethods = false)
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}

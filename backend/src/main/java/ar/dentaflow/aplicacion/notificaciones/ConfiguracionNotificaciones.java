package ar.dentaflow.aplicacion.notificaciones;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Propiedades de enlaces y de envío, y los hilos del envío de emails (design D5 y D7). */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PropiedadesEnlaces.class, PropiedadesEnvio.class})
public class ConfiguracionNotificaciones {

    private static final int HILOS = 2;
    private static final int CAPACIDAD_DE_COLA = 100;

    @Bean
    EjecutorDeEmails ejecutorDeEmails(PropiedadesEnvio propiedades) {
        return propiedades.asincronico() ? EjecutorDeEmails.conHilos(HILOS, CAPACIDAD_DE_COLA)
                : EjecutorDeEmails.sincronico();
    }
}

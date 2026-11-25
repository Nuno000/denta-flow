package ar.dentaflow.infraestructura.seguridad;

import ar.dentaflow.aplicacion.usuarios.LimitadorDeIntentosDeIngreso;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Arma el límite de intentos de ingreso con los valores de {@code app.auth} (design D7). */
@Configuration(proxyBeanMethods = false)
public class ConfiguracionIngreso {

    @Bean
    LimitadorDeIntentosDeIngreso limitadorDeIntentosDeIngreso(Clock reloj, PropiedadesAuth propiedades) {
        return new LimitadorDeIntentosDeIngreso(reloj, propiedades.intentosMaximos(), propiedades.ventanaIntentos());
    }
}

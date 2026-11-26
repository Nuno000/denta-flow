package ar.dentaflow.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.DentaFlowApplication;
import ar.dentaflow.infraestructura.seed.SeedRecursosDemo;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spec configuracion-entorno (C-19), "Perfil de despliegue": el perfil {@code prod} toma todo de variables
 * de entorno, no carga la demo salvo {@code APP_SEED_DEMO=true} y falla nombrando la variable que falta.
 * Se arranca la aplicación real con el perfil {@code prod} contra H2 en modo MySQL (sin red ni Docker).
 */
class PerfilProdTest {

    private static final String SECRETO = "clave-solo-para-pruebas-del-perfil-prod-0123456789";

    private static List<String> variablesBase() {
        List<String> variables = new ArrayList<>(List.of(
                "SPRING_DATASOURCE_URL=jdbc:h2:mem:prod-" + UUID.randomUUID()
                        + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "SPRING_DATASOURCE_USERNAME=sa",
                "SPRING_DATASOURCE_PASSWORD=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "AUTH_SECRET=" + SECRETO,
                "app.seed.admin=false",
                "server.port=0"));
        return variables;
    }

    /**
     * Surefire activa el perfil {@code test} por propiedad de sistema; los argumentos de línea de comandos
     * tienen más prioridad, así que el perfil queda solo en {@code prod}, como en Render.
     */
    private static ConfigurableApplicationContext arrancar(List<String> variables) {
        List<String> argumentos = new ArrayList<>();
        argumentos.add("--spring.profiles.active=prod");
        variables.forEach(variable -> argumentos.add("--" + variable));
        return new SpringApplicationBuilder(DentaFlowApplication.class).run(argumentos.toArray(String[]::new));
    }

    private static int profesionales(ConfigurableApplicationContext contexto) {
        Integer cantidad = contexto.getBean(JdbcTemplate.class)
                .queryForObject("SELECT COUNT(*) FROM profesional", Integer.class);
        return cantidad == null ? 0 : cantidad;
    }

    @Test
    void sinSeedDemoNoCargaDatosDeDemostracion() {
        try (ConfigurableApplicationContext contexto = arrancar(variablesBase())) {
            assertThat(profesionales(contexto)).isZero();
            assertThat(contexto.getBeansOfType(SeedRecursosDemo.class)).isEmpty();
        }
    }

    @Test
    void conSeedDemoCargaLaDemoUnaSolaVez() throws Exception {
        List<String> variables = variablesBase();
        variables.add("APP_SEED_DEMO=true");
        try (ConfigurableApplicationContext contexto = arrancar(variables)) {
            assertThat(profesionales(contexto)).isEqualTo(3);

            contexto.getBean(SeedRecursosDemo.class).run(new DefaultApplicationArguments());

            assertThat(profesionales(contexto)).isEqualTo(3);
        }
    }

    @Test
    void sinUrlDeLaBaseFallaNombrandoLaVariable() {
        List<String> variables = variablesBase();
        variables.removeIf(variable -> variable.startsWith("SPRING_DATASOURCE_URL="));

        assertThatThrownBy(() -> arrancar(variables).close())
                .hasStackTraceContaining("SPRING_DATASOURCE_URL");
    }

    @Test
    void sinClaveDeFirmaFallaNombrandoLaVariableSinMostrarValores() {
        List<String> variables = variablesBase();
        variables.removeIf(variable -> variable.startsWith("AUTH_SECRET="));

        assertThatThrownBy(() -> arrancar(variables).close())
                .hasStackTraceContaining("AUTH_SECRET")
                .satisfies(error -> assertThat(stackTrace(error)).doesNotContain("SPRING_DATASOURCE_PASSWORD="));
    }

    private static String stackTrace(Throwable error) {
        java.io.StringWriter texto = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(texto));
        return texto.toString();
    }
}

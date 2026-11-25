package ar.dentaflow.infraestructura.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Spec recursos-consultorio — "Datos de demostración": el seed nunca corre en el perfil de pruebas. */
@SpringBootTest
@ActiveProfiles("test")
class SeedNoCorreEnTestTest {

    @Autowired
    private ApplicationContext contexto;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void elPerfilTestNoDefineElBeanDelSeed() {
        assertThat(contexto.getBeansOfType(SeedRecursosDemo.class)).isEmpty();
    }

    @Test
    void lasTablasDeRecursosEstanVacias() {
        for (String tabla : new String[] {"profesional", "sillon", "prestacion"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class)).as(tabla).isZero();
        }
    }
}

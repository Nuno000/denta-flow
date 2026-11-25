package ar.dentaflow.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec configuracion-entorno — "Migraciones aplicadas al arrancar" y spec zona-horaria-consultorio —
 * "Almacenamiento en UTC": Hibernate solo valida, sin open-in-view y con JDBC en UTC.
 */
@SpringBootTest
@ActiveProfiles("test")
class PropiedadesJpaTest {

    @Autowired
    private Environment entorno;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void hibernateSoloValidaElEsquema() {
        assertThat(entityManagerFactory.getProperties()).containsEntry("hibernate.hbm2ddl.auto", "validate");
    }

    @Test
    void openInViewDesactivado() {
        assertThat(entorno.getProperty("spring.jpa.open-in-view")).isEqualTo("false");
    }

    @Test
    void jdbcTrabajaEnUtc() {
        assertThat(entityManagerFactory.getProperties()).containsEntry("hibernate.jdbc.time_zone", "UTC");
    }
}

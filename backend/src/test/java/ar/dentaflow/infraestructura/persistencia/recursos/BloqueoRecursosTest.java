package ar.dentaflow.infraestructura.persistencia.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec recursos-consultorio — "Recursos bloqueables". Prueba de humo: el mapeo permite bloquear
 * una fila de profesional y después una de sillón (el orden de C-09). La concurrencia real se
 * prueba en C-09 contra MySQL.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BloqueoRecursosTest {

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private EntityManager em;

    @Test
    void sePuedeBloquearElProfesionalYDespuesElSillon() {
        Long profesionalId = profesionales.guardar(
                Profesional.nuevo("Laura", "Ejemplo", null, "laura@example.com")).getId();
        Long sillonId = sillones.guardar(Sillon.nuevo("Sillón 1")).getId();
        em.flush();
        em.clear();

        Profesional profesional = em.find(Profesional.class, profesionalId, LockModeType.PESSIMISTIC_WRITE);
        Sillon sillon = em.find(Sillon.class, sillonId, LockModeType.PESSIMISTIC_WRITE);

        assertThat(profesional).isNotNull();
        assertThat(sillon).isNotNull();
        assertThat(em.getLockMode(profesional)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(em.getLockMode(sillon)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}

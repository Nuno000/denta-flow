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
 * Spec recursos-consultorio — "Recursos bloqueables": los puertos permiten bloquear para escritura
 * la fila del profesional y la del sillón ({@code SELECT ... FOR UPDATE}), que usan C-06 (altas de
 * horarios y bloqueos) y C-09 (turnos) en el orden profesional → sillón.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BloquearPorIdTest {

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private EntityManager em;

    @Test
    void bloqueaProfesionalYDespuesSillonParaEscritura() {
        Long profesionalId = profesionales.guardar(
                Profesional.nuevo("Laura", "Ejemplo", null, "laura.lock@example.com")).getId();
        Long sillonId = sillones.guardar(Sillon.nuevo("Sillón lock")).getId();
        em.flush();
        em.clear();

        Profesional profesional = profesionales.bloquearPorId(profesionalId).orElseThrow();
        Sillon sillon = sillones.bloquearPorId(sillonId).orElseThrow();

        assertThat(em.getLockMode(profesional)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(em.getLockMode(sillon)).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    @Test
    void inexistenteDevuelveVacio() {
        assertThat(profesionales.bloquearPorId(-1L)).isEmpty();
        assertThat(sillones.bloquearPorId(-1L)).isEmpty();
    }
}

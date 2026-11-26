package ar.dentaflow.infraestructura.persistencia.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.turnos.CerrojoDeRecursos;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Spec turnos — "Bloqueo de recursos en orden fijo": el cerrojo toma la fila de profesional y de sillón (design D5). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CerrojoDeRecursosJpa.class)
class CerrojoDeRecursosTest {

    @Autowired
    private CerrojoDeRecursos cerrojo;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void bloqueaUnProfesionalYDespuesUnSillonExistentes() {
        EscenarioDeAgendaJdbc escenario = new EscenarioDeAgendaJdbc(jdbc);
        long profesional = escenario.profesional();
        long sillon = escenario.sillon();

        cerrojo.bloquearProfesional(profesional);
        cerrojo.bloquearSillon(sillon);

        assertThat(profesional).isPositive();
    }

    @Test
    void unProfesionalInexistenteLanzaRecursoNoEncontrado() {
        assertThatThrownBy(() -> cerrojo.bloquearProfesional(999_999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("profesional");
    }

    @Test
    void unSillonInexistenteLanzaRecursoNoEncontrado() {
        assertThatThrownBy(() -> cerrojo.bloquearSillon(999_999_999L))
                .isInstanceOf(RecursoNoEncontradoException.class).hasMessageContaining("sillón");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void sinTransaccionSeRechaza() {
        assertThatThrownBy(() -> cerrojo.bloquearProfesional(1L))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}

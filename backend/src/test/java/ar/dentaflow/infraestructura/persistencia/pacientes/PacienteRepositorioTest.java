package ar.dentaflow.infraestructura.persistencia.pacientes;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Spec pacientes — persistencia y "DNI único con deduplicación" (design D10). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PacienteRepositorioTest {

    @Autowired
    private PacienteRepositorio pacientes;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private String dniLibre() {
        return new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
    }

    @Test
    void guardaYRecuperaTodosLosCampos() {
        String dni = dniLibre();
        Instant consentimiento = Instant.parse("2026-12-01T12:05:00.123456Z");
        Paciente nuevo = Paciente.nuevo(dni, "Ana", "Ejemplo", "+54 11 5555-0000", "Ana@Example.com",
                Instant.parse("2026-12-01T12:00:00Z"));
        nuevo.registrarConsentimiento(consentimiento);

        Long id = pacientes.guardar(nuevo).getId();
        em.flush();
        em.clear();

        Paciente leido = pacientes.buscarPorId(id).orElseThrow();
        assertThat(leido.getDni()).isEqualTo(dni);
        assertThat(leido.getNombre()).isEqualTo("Ana");
        assertThat(leido.getApellido()).isEqualTo("Ejemplo");
        assertThat(leido.getTelefono()).isEqualTo("+541155550000");
        assertThat(leido.getEmail()).isEqualTo("ana@example.com");
        assertThat(leido.getCreadoEn()).isEqualTo(Instant.parse("2026-12-01T12:00:00Z"));
        assertThat(leido.getConsentimientoPrivacidadEn()).isEqualTo(consentimiento);
    }

    @Test
    void sinConsentimientoVuelveVacio() {
        Long id = pacientes.guardar(Paciente.nuevo(dniLibre(), "Ana", "Ejemplo", "1155550000", "a@example.com",
                Instant.parse("2026-12-01T12:00:00Z"))).getId();
        em.flush();
        em.clear();

        assertThat(pacientes.buscarPorId(id).orElseThrow().getConsentimientoPrivacidadEn()).isNull();
    }

    @Test
    void buscarPorDniEncuentraAlPacienteConElDniNormalizado() {
        String dni = dniLibre();
        Long id = pacientes.guardar(Paciente.nuevo(dni, "Ana", "Ejemplo", "1155550000", "a@example.com",
                Instant.parse("2026-12-01T12:00:00Z"))).getId();
        em.flush();
        em.clear();

        assertThat(pacientes.buscarPorDni(dni)).map(Paciente::getId).contains(id);
    }

    @Test
    void buscarPorDniInexistenteDevuelveVacio() {
        assertThat(pacientes.buscarPorDni(dniLibre())).isEmpty();
    }

    @Test
    void buscarPorIdInexistenteDevuelveVacio() {
        assertThat(pacientes.buscarPorId(999_999_999L)).isEmpty();
    }
}

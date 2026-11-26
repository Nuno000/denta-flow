package ar.dentaflow.aplicacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec privacidad-datos-personales (C-15) con transacciones reales (el paciente se registra en su propia
 * transacción, C-09): "El DNI original queda libre", "Registro online sin aceptación", "Datos mínimos del
 * paciente" y "Sin datos personales en logs". DNI y emails únicos por ejecución.
 */
@SpringBootTest
@ActiveProfiles("test")
class PrivacidadPacienteIntegracionTest {

    private static final Instant ACEPTACION = Instant.parse("2026-12-01T12:00:00Z");

    @Autowired
    private ServicioDePacientes pacientes;

    @Autowired
    private ServicioDeAnonimizacion anonimizacion;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    private DatosPaciente datos(String dni, String email, Instant aceptacion) {
        return new DatosPaciente(dni, "Ana", "Prueba", "1155550000", email, aceptacion);
    }

    private String email() {
        return "ana-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void despuesDeLaBajaElMismoDniCreaUnPacienteNuevo() {
        String dni = new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
        Paciente original = pacientes.registrarDesdeReservaOnline(datos(dni, email(), ACEPTACION));
        long adminId = usuarios.crear(email(), "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();

        anonimizacion.anonimizar(original.getId(), adminId);
        Paciente nuevo = pacientes.registrarDesdeReservaOnline(datos(dni, email(), ACEPTACION.plusSeconds(60)));

        assertThat(nuevo.getId()).isNotEqualTo(original.getId());
        assertThat(nuevo.getDni()).isEqualTo(dni);
        assertThat(jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, original.getId())).isNull();
    }

    @Test
    void elRegistroOnlineSinAceptacionNoCreaPacienteNiDejaDatosEnElLog() {
        String dni = new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
        String email = email();
        ListAppender<ILoggingEvent> logs = new ListAppender<>();
        Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        logs.start();
        raiz.addAppender(logs);
        try {
            assertThatThrownBy(() -> pacientes.registrarDesdeReservaOnline(datos(dni, email, null)))
                    .isInstanceOf(ValidacionDeDominioException.class)
                    .hasMessageNotContaining(dni);
        } finally {
            raiz.detachAppender(logs);
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni = ?", Long.class, dni)).isZero();
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(m -> m.contains(dni) || m.contains(email));
    }

    @Test
    void elPacienteSoloTieneLasColumnasPermitidas() {
        List<String> columnas = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE LOWER(table_name) = 'paciente'", String.class)
                .stream().map(c -> c.toLowerCase(Locale.ROOT)).sorted().toList();

        assertThat(columnas).containsExactlyInAnyOrder("id", "dni", "nombre", "apellido", "telefono", "email",
                "consentimiento_privacidad_en", "creado_en", "anonimizado_en", "anonimizado_por");
    }
}

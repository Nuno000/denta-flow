package ar.dentaflow.aplicacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.persistencia.pacientes.PacienteRepositorioJpa;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec pacientes — "Dos altas simultáneas del mismo DNI" contra H2. Sin {@code @Transactional} en la clase. La
 * carrera se simula así: una decoración del repositorio, justo después de que el servicio busca el DNI y no lo
 * encuentra, inserta ese DNI desde otra conexión (como lo haría otra transacción ya confirmada).
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicioDePacientesIntegracionTest {

    /** DNI que el repositorio decorado "roba" en la próxima búsqueda; nulo si no hay carrera simulada. */
    private static final AtomicReference<String> DNI_A_ROBAR = new AtomicReference<>();

    @TestConfiguration
    static class CarreraSimulada {

        @Bean
        @Primary
        PacienteRepositorio repositorioConCarrera(PacienteRepositorioJpa real, DataSource dataSource) {
            return new PacienteRepositorio() {
                @Override
                public Paciente guardar(Paciente paciente) {
                    return real.guardar(paciente);
                }

                @Override
                public Optional<Paciente> buscarPorId(Long id) {
                    return real.buscarPorId(id);
                }

                @Override
                public Optional<Paciente> buscarPorDni(String dniNormalizado) {
                    Optional<Paciente> encontrado = real.buscarPorDni(dniNormalizado);
                    String aRobar = DNI_A_ROBAR.get();
                    if (encontrado.isEmpty() && dniNormalizado.equals(aRobar) && DNI_A_ROBAR.compareAndSet(aRobar, null)) {
                        insertarDesdeOtraConexion(dataSource, dniNormalizado);
                    }
                    return encontrado;
                }
            };
        }
    }

    /**
     * Conexión propia con autocommit, fuera de la transacción del servicio: si usara el JdbcTemplate del contexto
     * compartiría esa transacción y el "ganador" se revertiría junto con el intento fallido.
     */
    private static void insertarDesdeOtraConexion(DataSource dataSource, String dni) {
        try (Connection conexion = dataSource.getConnection();
                PreparedStatement sentencia = conexion.prepareStatement(
                        "INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en) "
                                + "VALUES (?, 'Ganador', 'Previo', '1100000000', 'ganador@example.com', ?)")) {
            sentencia.setString(1, dni);
            sentencia.setTimestamp(2, Timestamp.from(Instant.parse("2026-11-01T10:00:00Z")));
            sentencia.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Autowired
    private ServicioDePacientes servicio;

    @Autowired
    private JdbcTemplate jdbc;

    private String dni;

    @AfterEach
    void limpiar() {
        DNI_A_ROBAR.set(null);
        if (dni != null) {
            jdbc.update("DELETE FROM paciente WHERE dni = ?", dni);
        }
    }

    private Long filas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni = ?", Long.class, dni);
    }

    @Test
    void siOtraTransaccionInsertoElMismoDniElServicioDevuelveEsePacienteYQuedaUnaSolaFila() {
        dni = new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
        DNI_A_ROBAR.set(dni);

        Paciente devuelto = servicio.registrarDesdeReservaOnline(
                new DatosPaciente(dni, "Ana", "Ejemplo", "1155550000", "ana@example.com", null));

        assertThat(DNI_A_ROBAR.get()).as("la carrera ocurrió").isNull();
        assertThat(devuelto.getId()).isNotNull();
        assertThat(devuelto.getNombre()).isEqualTo("Ganador");
        assertThat(filas()).isEqualTo(1L);
    }

    @Test
    void desdeRecepcionTrasLaCarreraReemplazaElContactoDelGanador() {
        dni = new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
        DNI_A_ROBAR.set(dni);

        Paciente devuelto = servicio.registrarDesdeRecepcion(
                new DatosPaciente(dni, "Ana", "Ejemplo", "1155550000", "ana@example.com", null));

        assertThat(devuelto.getNombre()).isEqualTo("Ana");
        assertThat(devuelto.getEmail()).isEqualTo("ana@example.com");
        assertThat(filas()).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT email FROM paciente WHERE dni = ?", String.class, dni))
                .isEqualTo("ana@example.com");
    }

    @Test
    void sinCarreraElAltaDejaUnaFilaYUnaSegundaLlamadaLaReutiliza() {
        dni = new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
        DatosPaciente datos = new DatosPaciente(dni, "Ana", "Ejemplo", "1155550000", "ana@example.com", null);

        Long primero = servicio.registrarDesdeReservaOnline(datos).getId();
        Long segundo = servicio.registrarDesdeReservaOnline(datos).getId();

        assertThat(segundo).isEqualTo(primero);
        assertThat(filas()).isEqualTo(1L);
    }
}

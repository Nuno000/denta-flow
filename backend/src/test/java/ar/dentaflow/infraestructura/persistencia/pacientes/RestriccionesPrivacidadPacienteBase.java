package ar.dentaflow.infraestructura.persistencia.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec privacidad-datos-personales (C-15), "Anonimización del paciente" — base: restricciones de {@code V015_1}
 * insertando por JDBC. El DNI solo puede quedar vacío en un paciente anonimizado, y la anonimización guarda
 * quién y cuándo, siempre juntos. Las subclases eligen el motor. Revierte todo al terminar.
 */
@Transactional
abstract class RestriccionesPrivacidadPacienteBase {

    private static final Timestamp AHORA = Timestamp.from(Instant.parse("2026-12-01T12:00:00Z"));

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    private Long adminId;

    @BeforeEach
    void crearAdministrador() {
        String email = "admin-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        adminId = usuarios.crear(email, "contrasena-valida", Rol.ADMINISTRADOR, null).usuario().getId();
    }

    private void insertar(String dni, Timestamp anonimizadoEn, Long anonimizadoPor) {
        String email = "paciente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        jdbc.update("INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en, anonimizado_en, "
                + "anonimizado_por) VALUES (?, 'Ana', 'Ejemplo', '1155550000', ?, ?, ?, ?)",
                dni, email, AHORA, anonimizadoEn, anonimizadoPor);
    }

    @Test
    void aceptaVariosPacientesAnonimizadosSinDni() {
        insertar(null, AHORA, adminId);
        insertar(null, AHORA, adminId);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni IS NULL", Long.class))
                .isGreaterThanOrEqualTo(2L);
    }

    @Test
    void rechazaUnPacienteSinDniQueNoEstaAnonimizado() {
        assertRechazadoPor("ck_paciente_dni_o_anonimizado", () -> insertar(null, null, null));
    }

    @Test
    void rechazaLaAnonimizacionSinAutorOSinFecha() {
        String dni = new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
        assertRechazadoPor("ck_paciente_anonimizado", () -> insertar(dni, AHORA, null));
        assertRechazadoPor("ck_paciente_anonimizado", () -> insertar(dni, null, adminId));
    }

    @Test
    void rechazaUnAutorDeAnonimizacionInexistente() {
        assertRechazadoPor("fk_paciente_anonimizado_por", () -> insertar(null, AHORA, 999_999L));
    }

    /** Mismo criterio que {@code RestriccionesPacienteBase}: el rechazo nombra la restricción, sea cual sea la clase. */
    private void assertRechazadoPor(String nombre, ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT)).contains(nombre));
    }
}

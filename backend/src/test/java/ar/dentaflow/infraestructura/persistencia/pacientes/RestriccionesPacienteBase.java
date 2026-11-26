package ar.dentaflow.infraestructura.persistencia.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec pacientes — "Integridad de pacientes en la base": restricciones del esquema {@code V009_1} insertando por
 * JDBC. Las subclases eligen el motor (H2 en modo MySQL o MySQL 8 real). Revierte todo al terminar.
 */
@Transactional
abstract class RestriccionesPacienteBase {

    private static final Timestamp AHORA = Timestamp.from(Instant.parse("2026-12-01T12:00:00Z"));

    @Autowired
    protected JdbcTemplate jdbc;

    private String dniLibre() {
        return new EscenarioDeAgendaJdbc(jdbc).dniNuevo();
    }

    private void insertar(String dni, String nombre, String apellido, String telefono, String email) {
        jdbc.update("INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en) VALUES (?, ?, ?, ?, ?, ?)",
                dni, nombre, apellido, telefono, email, AHORA);
    }

    private String email() {
        return "paciente-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    @Test
    void aceptaUnaFilaValida() {
        String dni = dniLibre();

        insertar(dni, "Ana", "Ejemplo", "1155550000", email());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni = ?", Long.class, dni)).isEqualTo(1L);
    }

    @Test
    void aceptaUnDniDeSieteDigitosYElConsentimientoVacio() {
        String dni = "1" + dniLibre().substring(1, 7);
        jdbc.update("DELETE FROM paciente WHERE dni = ?", dni);

        insertar(dni, "Ana", "Ejemplo", "1155550000", email());

        assertThat(jdbc.queryForObject("SELECT consentimiento_privacidad_en FROM paciente WHERE dni = ?",
                Timestamp.class, dni)).isNull();
    }

    @Test
    void rechazaCadaDatoObligatorioNulo() {
        assertRechazadoPor("dni", () -> insertar(null, "Ana", "Ejemplo", "1155550000", email()));
        assertRechazadoPor("nombre", () -> insertar(dniLibre(), null, "Ejemplo", "1155550000", email()));
        assertRechazadoPor("apellido", () -> insertar(dniLibre(), "Ana", null, "1155550000", email()));
        assertRechazadoPor("telefono", () -> insertar(dniLibre(), "Ana", "Ejemplo", null, email()));
        assertRechazadoPor("email", () -> insertar(dniLibre(), "Ana", "Ejemplo", "1155550000", null));
    }

    @Test
    void rechazaUnDniRepetido() {
        String dni = dniLibre();
        insertar(dni, "Ana", "Ejemplo", "1155550000", email());

        assertRechazadoPor("uk_paciente_dni", () -> insertar(dni, "Otra", "Persona", "1155551111", email()));
    }

    @Test
    void rechazaUnDniDeSeisONueveCaracteres() {
        assertRechazadoPor("ck_paciente_dni_largo", () -> insertar("123456", "Ana", "Ejemplo", "1155550000", email()));
        // Con 9 caracteres ya no entra en VARCHAR(8): lo frena el largo de la columna antes que el CHECK.
        assertRechazadoPor("dni", () -> insertar("123456789", "Ana", "Ejemplo", "1155550000", email()));
    }

    /**
     * H2 y MySQL traducen distinto cada violación (MySQL deja el CHECK como {@code UncategorizedSQLException}): se
     * exige el rechazo nombrando la restricción o la columna, sin atarse a la subclase.
     */
    private void assertRechazadoPor(String nombre, ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT)).contains(nombre));
    }
}

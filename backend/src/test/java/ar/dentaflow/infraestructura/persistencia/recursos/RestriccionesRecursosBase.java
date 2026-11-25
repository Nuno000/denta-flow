package ar.dentaflow.infraestructura.persistencia.recursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec recursos-consultorio — restricciones del esquema {@code V003_1}. Las subclases eligen el
 * motor (H2 en modo MySQL o MySQL 8 real). No asume tablas vacías: usa nombres únicos por
 * ejecución y revierte todo al terminar cada prueba.
 */
@Transactional
abstract class RestriccionesRecursosBase {

    @Autowired
    protected JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    private String unico(String base) {
        return base + "-" + sufijo;
    }

    private long insertarPrestacion(String nombre, Integer duracion) {
        jdbc.update("INSERT INTO prestacion (nombre, duracion_minutos) VALUES (?, ?)", nombre, duracion);
        return jdbc.queryForObject("SELECT id FROM prestacion WHERE nombre = ?", Long.class, nombre);
    }

    private long insertarProfesional(String email, String matricula) {
        jdbc.update("INSERT INTO profesional (nombre, apellido, matricula, email) VALUES ('N', 'A', ?, ?)",
                matricula, email);
        return jdbc.queryForObject("SELECT id FROM profesional WHERE email = ?", Long.class, email);
    }

    @Test
    void lasCuatroTablasExisten() {
        for (String tabla : new String[] {"profesional", "sillon", "prestacion", "profesional_prestacion"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Long.class))
                    .as(tabla).isNotNull();
        }
    }

    /**
     * Spring traduce la violación del CHECK a {@code DataIntegrityViolationException} en H2, pero en
     * MySQL 8 (error 3819, SQL state HY000) la deja como {@code UncategorizedSQLException}. Por eso
     * se exige que la base rechace el insert nombrando la restricción, sin atarse a la subclase.
     */
    @Test
    void rechazaDuracionCeroONegativa() {
        assertRechazadoPorCheckDeDuracion(() -> insertarPrestacion(unico("cero"), 0));
        assertRechazadoPorCheckDeDuracion(() -> insertarPrestacion(unico("negativa"), -5));
    }

    private void assertRechazadoPorCheckDeDuracion(ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT))
                        .contains("ck_prestacion_duracion_positiva"));
    }

    @Test
    void rechazaDuracionNula() {
        assertThatThrownBy(() -> insertarPrestacion(unico("nula"), null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aceptaDuracionPositiva() {
        long id = insertarPrestacion(unico("ok"), 1);

        assertThat(jdbc.queryForObject("SELECT duracion_minutos FROM prestacion WHERE id = ?", Integer.class, id))
                .isEqualTo(1);
    }

    @Test
    void losRecursosNacenActivosPorDefecto() {
        long prestacion = insertarPrestacion(unico("defecto"), 30);
        long profesional = insertarProfesional(unico("defecto") + "@example.com", null);
        jdbc.update("INSERT INTO sillon (nombre) VALUES (?)", unico("defecto"));

        assertThat(jdbc.queryForObject("SELECT activa FROM prestacion WHERE id = ?", Boolean.class, prestacion)).isTrue();
        assertThat(jdbc.queryForObject("SELECT activo FROM profesional WHERE id = ?", Boolean.class, profesional)).isTrue();
        assertThat(jdbc.queryForObject("SELECT activo FROM sillon WHERE nombre = ?", Boolean.class, unico("defecto"))).isTrue();
    }

    @Test
    void rechazaEmailDeProfesionalRepetido() {
        String email = unico("repetido") + "@example.com";
        insertarProfesional(email, null);

        assertThatThrownBy(() -> insertarProfesional(email, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaMatriculaRepetidaPeroAceptaVariasSinMatricula() {
        String matricula = unico("MP");
        insertarProfesional(unico("a") + "@example.com", matricula);

        assertThatThrownBy(() -> insertarProfesional(unico("b") + "@example.com", matricula))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void variosProfesionalesSinMatriculaSeAceptan() {
        insertarProfesional(unico("x") + "@example.com", null);
        insertarProfesional(unico("y") + "@example.com", null);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profesional WHERE email IN (?, ?)", Long.class,
                unico("x") + "@example.com", unico("y") + "@example.com")).isEqualTo(2L);
    }

    @Test
    void rechazaNombreDeSillonRepetido() {
        jdbc.update("INSERT INTO sillon (nombre) VALUES (?)", unico("sillon"));

        assertThatThrownBy(() -> jdbc.update("INSERT INTO sillon (nombre) VALUES (?)", unico("sillon")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaNombreDePrestacionRepetido() {
        insertarPrestacion(unico("prestacion"), 30);

        assertThatThrownBy(() -> insertarPrestacion(unico("prestacion"), 45))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaAsociarUnaPrestacionInexistente() {
        long profesional = insertarProfesional(unico("fk") + "@example.com", null);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO profesional_prestacion (profesional_id, prestacion_id) VALUES (?, ?)",
                profesional, Long.MAX_VALUE))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaAsociarDosVecesLaMismaPrestacion() {
        long profesional = insertarProfesional(unico("pk") + "@example.com", null);
        long prestacion = insertarPrestacion(unico("pk"), 30);
        jdbc.update("INSERT INTO profesional_prestacion (profesional_id, prestacion_id) VALUES (?, ?)",
                profesional, prestacion);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO profesional_prestacion (profesional_id, prestacion_id) VALUES (?, ?)",
                profesional, prestacion))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void noSePuedeBorrarUnaPrestacionAsociada() {
        long profesional = insertarProfesional(unico("del") + "@example.com", null);
        long prestacion = insertarPrestacion(unico("del"), 30);
        jdbc.update("INSERT INTO profesional_prestacion (profesional_id, prestacion_id) VALUES (?, ?)",
                profesional, prestacion);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM prestacion WHERE id = ?", prestacion))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("DELETE FROM profesional WHERE id = ?", profesional))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

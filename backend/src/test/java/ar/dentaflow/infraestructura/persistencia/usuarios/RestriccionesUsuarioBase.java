package ar.dentaflow.infraestructura.persistencia.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Specs gestion-usuarios ("Catálogo de roles", "Email único", "Vínculo con el profesional") —
 * restricciones del esquema {@code V004_1}. Las subclases eligen el motor. No asume tablas vacías:
 * usa datos únicos por ejecución y revierte todo al terminar cada prueba.
 */
@Transactional
abstract class RestriccionesUsuarioBase {

    private static final String HASH = "$2a$10$hashDePrueba";

    @Autowired
    protected JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    private String email(String base) {
        return base + "-" + sufijo + "@example.com";
    }

    private long insertarProfesional() {
        String email = email("prof-" + UUID.randomUUID().toString().substring(0, 6));
        jdbc.update("INSERT INTO profesional (nombre, apellido, email) VALUES ('N', 'A', ?)", email);
        return jdbc.queryForObject("SELECT id FROM profesional WHERE email = ?", Long.class, email);
    }

    private void insertarUsuario(String email, String rol, Long profesionalId) {
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol, profesional_id) VALUES (?, ?, ?, ?)",
                email, HASH, rol, profesionalId);
    }

    @Test
    void elCatalogoTieneLosTresRolesConSuNombreVisible() {
        List<Map<String, Object>> filas = jdbc.queryForList("SELECT codigo, nombre FROM rol ORDER BY codigo");

        assertThat(filas).extracting(f -> f.get("codigo"), f -> f.get("nombre")).containsExactly(
                org.assertj.core.groups.Tuple.tuple("administrador", "Administrador"),
                org.assertj.core.groups.Tuple.tuple("profesional", "Profesional"),
                org.assertj.core.groups.Tuple.tuple("recepcion", "Recepción"));
    }

    @Test
    void activoValeTruePorDefecto() {
        String email = email("activo");
        insertarUsuario(email, "recepcion", null);

        assertThat(jdbc.queryForObject("SELECT activo FROM usuario WHERE email = ?", Boolean.class, email))
                .isTrue();
    }

    @Test
    void rechazaEmailRepetido() {
        String email = email("repetido");
        insertarUsuario(email, "recepcion", null);

        assertThatThrownBy(() -> insertarUsuario(email, "administrador", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaRolFueraDelCatalogo() {
        assertThatThrownBy(() -> insertarUsuario(email("super"), "superusuario", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaProfesionalSinProfesionalVinculado() {
        assertRechazadoPorCheckDeVinculo(() -> insertarUsuario(email("prof-sin"), "profesional", null));
    }

    @Test
    void rechazaRecepcionConProfesionalVinculado() {
        long profesional = insertarProfesional();

        assertRechazadoPorCheckDeVinculo(() -> insertarUsuario(email("recep-con"), "recepcion", profesional));
    }

    @Test
    void unProfesionalTieneComoMaximoUnUsuario() {
        long profesional = insertarProfesional();
        insertarUsuario(email("prof-1"), "profesional", profesional);

        assertThatThrownBy(() -> insertarUsuario(email("prof-2"), "profesional", profesional))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void variosUsuariosSinProfesionalSeAceptan() {
        insertarUsuario(email("sin-1"), "recepcion", null);
        insertarUsuario(email("sin-2"), "administrador", null);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuario WHERE email IN (?, ?)", Long.class,
                email("sin-1"), email("sin-2"))).isEqualTo(2L);
    }

    @Test
    void rechazaProfesionalInexistente() {
        assertThatThrownBy(() -> insertarUsuario(email("fk"), "profesional", 999_999_999L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Como en C-03: H2 traduce la violación del CHECK a {@code DataIntegrityViolationException}, pero
     * MySQL 8 la deja como {@code UncategorizedSQLException}. Se exige el rechazo nombrando la restricción.
     */
    private void assertRechazadoPorCheckDeVinculo(ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT))
                        .contains("ck_usuario_rol_profesional"));
    }
}

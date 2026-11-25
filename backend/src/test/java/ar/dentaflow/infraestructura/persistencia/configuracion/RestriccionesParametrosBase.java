package ar.dentaflow.infraestructura.persistencia.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec parametros-consultorio — valores por defecto provisorios, integridad en la base y caracteres
 * del español ({@code V007_1}). Las subclases eligen el motor. Todo se revierte al terminar cada prueba.
 */
@Transactional
abstract class RestriccionesParametrosBase {

    private static final String ANTICIPACION = "anticipacion-minima-horas";
    private static final String TEXTO = "aviso-privacidad-texto";
    private static final String LEYENDA = "aviso-privacidad-leyenda";

    @Autowired
    protected JdbcTemplate jdbc;

    private long usuarioNuevo() {
        String email = "param-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES (?, 'x', 'administrador')", email);
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    private String valor(String clave) {
        return jdbc.queryForObject("SELECT valor FROM parametro WHERE clave = ?", String.class, clave);
    }

    @Test
    void laBaseRecienMigradaTieneExactamenteLosTresParametrosProvisorios() {
        List<String> claves = jdbc.queryForList("SELECT clave FROM parametro", String.class);

        assertThat(claves).containsExactlyInAnyOrder(ANTICIPACION, TEXTO, LEYENDA);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parametro WHERE provisorio = TRUE", Long.class))
                .isEqualTo(3L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM parametro WHERE actualizado_en IS NULL AND actualizado_por IS NULL",
                Long.class)).isEqualTo(3L);
    }

    @Test
    void losValoresInicialesSonLosDelDiseno() {
        assertThat(valor(ANTICIPACION)).isEqualTo("24");
        assertThat(valor(LEYENDA)).isEqualTo("Leí y acepto el aviso de privacidad");
        assertThat(valor(TEXTO)).contains("borrador").contains("25.326");
        assertThat(valor(TEXTO).length()).isLessThanOrEqualTo(4000);
    }

    @Test
    void rechazaUnaSegundaFilaConLaMismaClave() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO parametro (clave, valor, provisorio) VALUES (?, '48', TRUE)", ANTICIPACION))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaValorOProvisorioNulos() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO parametro (clave, valor, provisorio) VALUES ('otra-1', NULL, TRUE)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO parametro (clave, valor, provisorio) VALUES ('otra-2', '1', NULL)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaUnAutorQueNoEsUnUsuario() {
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE parametro SET actualizado_por = ?, actualizado_en = ? WHERE clave = ?",
                Long.MAX_VALUE, Timestamp.valueOf("2026-12-02 00:30:00"), ANTICIPACION))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * En H2 Spring traduce la violación del CHECK a {@code DataIntegrityViolationException}; en MySQL 8
     * llega como {@code UncategorizedSQLException}. Se exige que la base nombre la restricción.
     */
    @Test
    void rechazaAutorSinFechaYFechaSinAutor() {
        long usuario = usuarioNuevo();

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE parametro SET actualizado_por = ? WHERE clave = ?", usuario, ANTICIPACION))
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT))
                        .contains("ck_parametro_autor_y_fecha"));
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE parametro SET actualizado_en = ? WHERE clave = ?",
                Timestamp.valueOf("2026-12-02 00:30:00"), ANTICIPACION))
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT))
                        .contains("ck_parametro_autor_y_fecha"));
    }

    @Test
    void aceptaAutorYFechaJuntos() {
        long usuario = usuarioNuevo();

        int filas = jdbc.update("UPDATE parametro SET actualizado_por = ?, actualizado_en = ?, provisorio = FALSE "
                + "WHERE clave = ?", usuario, Timestamp.valueOf("2026-12-02 00:30:00"), ANTICIPACION);

        assertThat(filas).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT actualizado_por FROM parametro WHERE clave = ?", Long.class,
                ANTICIPACION)).isEqualTo(usuario);
    }

    @Test
    void guardaYDevuelveLosCaracteresDelEspanolSinAlterarlos() {
        String texto = "¿Aceptás el tratamiento de tus datos según la Ley 25.326? Niño, camión, ñandú.";

        jdbc.update("UPDATE parametro SET valor = ? WHERE clave = ?", texto, TEXTO);

        assertThat(valor(TEXTO)).isEqualTo(texto);
    }
}

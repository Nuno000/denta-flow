package ar.dentaflow.infraestructura.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Spec zona-horaria-consultorio — "Almacenamiento en UTC". Los instantes se guardan en
 * {@code DATETIME(6)} como hora UTC y se leen sin corrimiento, aunque la JVM esté en otra zona.
 * Las subclases eligen el motor (H2 en modo MySQL o MySQL 8 real).
 */
abstract class IdaYVueltaUtcBase {

    private static final String TABLA = "prueba_ida_vuelta_utc";

    @Autowired
    private JdbcTemplate jdbc;

    private TimeZone zonaOriginal;

    @BeforeEach
    void crearTablaTemporal() {
        zonaOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        jdbc.execute("DROP TABLE IF EXISTS " + TABLA);
        jdbc.execute("CREATE TABLE " + TABLA + " (id BIGINT AUTO_INCREMENT PRIMARY KEY, momento DATETIME(6) NOT NULL)");
    }

    @AfterEach
    void borrarTablaTemporal() {
        jdbc.execute("DROP TABLE IF EXISTS " + TABLA);
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    void guardaElInstanteEnUtcYLoLeeSinCorrimiento() {
        assertIdaYVuelta(Instant.parse("2026-12-01T12:00:00Z"), "2026-12-01 12:00:00");
    }

    @Test
    void conservaLosMicrosegundos() {
        assertIdaYVuelta(Instant.parse("2026-12-01T02:30:00.123456Z"), "2026-12-01 02:30:00.123456");
    }

    private void assertIdaYVuelta(Instant instante, String textoUtcEsperado) {
        jdbc.update("INSERT INTO " + TABLA + " (momento) VALUES (?)", LocalDateTime.ofInstant(instante, ZoneOffset.UTC));

        String guardado = jdbc.queryForObject("SELECT CAST(momento AS CHAR(26)) FROM " + TABLA, String.class);
        LocalDateTime leido = jdbc.queryForObject("SELECT momento FROM " + TABLA, LocalDateTime.class);

        assertThat(guardado).startsWith(textoUtcEsperado);
        assertThat(leido.toInstant(ZoneOffset.UTC)).isEqualTo(instante);
    }
}

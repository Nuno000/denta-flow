package ar.dentaflow.infraestructura.persistencia.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec notificaciones-email — "Una notificación por turno y tipo": restricciones de {@code V014_1} insertando por
 * JDBC (design D2). Las subclases eligen el motor. Revierte todo al terminar.
 */
@Transactional
abstract class RestriccionesNotificacionBase {

    private static final Timestamp AHORA = Timestamp.from(Instant.parse("2026-12-01T12:00:00Z"));

    @Autowired
    protected JdbcTemplate jdbc;

    private long turno;

    @BeforeEach
    void crearTurno() {
        turno = new EscenarioDeAgendaJdbc(jdbc).turnoValido();
    }

    private void insertar(long turnoId, String tipo, String canal, String estado, int intentos, String motivo,
            Timestamp enviadaEn) {
        jdbc.update("INSERT INTO notificacion (turno_id, tipo, canal, estado, intentos, motivo_fallo, creada_en, "
                        + "actualizada_en, enviada_en) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                turnoId, tipo, canal, estado, intentos, motivo, AHORA, AHORA, enviadaEn);
    }

    private void insertarValida(String tipo, String estado) {
        insertar(turno, tipo, "email", estado, 1, estado.equals("fallida") ? "conexion" : null,
                estado.equals("enviada") ? AHORA : null);
    }

    @Test
    void aceptaUnaConfirmacionYUnRecordatorioDelMismoTurno() {
        insertarValida("confirmacion", "enviada");
        insertarValida("recordatorio", "en_curso");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM notificacion WHERE turno_id = ?", Long.class, turno))
                .isEqualTo(2L);
    }

    @Test
    void aceptaLosCuatroEstados() {
        insertarValida("confirmacion", "fallida");
        jdbc.update("DELETE FROM notificacion WHERE turno_id = ?", turno);
        insertarValida("confirmacion", "omitida");
        jdbc.update("DELETE FROM notificacion WHERE turno_id = ?", turno);
        insertarValida("confirmacion", "en_curso");
        jdbc.update("DELETE FROM notificacion WHERE turno_id = ?", turno);
        insertarValida("confirmacion", "enviada");

        assertThat(jdbc.queryForObject("SELECT estado FROM notificacion WHERE turno_id = ?", String.class, turno))
                .isEqualTo("enviada");
    }

    @Test
    void rechazaUnaSegundaNotificacionDelMismoTurnoYTipo() {
        insertarValida("confirmacion", "enviada");

        assertRechazadoPor("uk_notificacion_turno_tipo", () -> insertarValida("confirmacion", "en_curso"));
    }

    @Test
    void rechazaUnTipoFueraDelCatalogo() {
        assertRechazadoPor("ck_notificacion_tipo", () -> insertarValida("reintento", "en_curso"));
    }

    @Test
    void rechazaUnCanalFueraDelCatalogo() {
        assertRechazadoPor("ck_notificacion_canal",
                () -> insertar(turno, "confirmacion", "sms", "en_curso", 0, null, null));
    }

    @Test
    void rechazaUnEstadoFueraDelCatalogo() {
        assertRechazadoPor("ck_notificacion_estado",
                () -> insertar(turno, "confirmacion", "email", "borrada", 0, null, null));
    }

    @Test
    void rechazaIntentosNegativos() {
        assertRechazadoPor("ck_notificacion_intentos",
                () -> insertar(turno, "confirmacion", "email", "en_curso", -1, null, null));
    }

    @Test
    void rechazaUnMotivoFueraDelCatalogo() {
        assertRechazadoPor("ck_notificacion_motivo",
                () -> insertar(turno, "confirmacion", "email", "fallida", 1, "timeout", null));
    }

    @Test
    void rechazaUnaEnviadaSinInstanteDeEnvio() {
        assertRechazadoPor("ck_notificacion_enviada",
                () -> insertar(turno, "confirmacion", "email", "enviada", 1, null, null));
    }

    @Test
    void rechazaUnInstanteDeEnvioSinEstarEnviada() {
        assertRechazadoPor("ck_notificacion_enviada",
                () -> insertar(turno, "confirmacion", "email", "en_curso", 1, null, AHORA));
    }

    @Test
    void rechazaUnaFallidaSinMotivo() {
        assertRechazadoPor("ck_notificacion_fallida",
                () -> insertar(turno, "confirmacion", "email", "fallida", 3, null, null));
    }

    @Test
    void rechazaUnMotivoSinEstarFallida() {
        assertRechazadoPor("ck_notificacion_fallida",
                () -> insertar(turno, "confirmacion", "email", "en_curso", 1, "conexion", null));
    }

    @Test
    void rechazaUnTurnoInexistente() {
        assertRechazadoPor("fk_notificacion_turno",
                () -> insertar(Long.MAX_VALUE, "confirmacion", "email", "en_curso", 0, null, null));
    }

    private void assertRechazadoPor(String restriccion, ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT)).contains(restriccion));
    }
}

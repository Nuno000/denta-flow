package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Restricciones del esquema {@code V006_1} (horarios de atención y bloqueos). Las subclases eligen
 * el motor (H2 en modo MySQL o MySQL 8 real). Crea sus propios recursos con nombres únicos y
 * revierte todo al terminar cada prueba.
 */
@Transactional
abstract class RestriccionesDisponibilidadBase {

    private static final Timestamp NUEVE = Timestamp.from(Instant.parse("2026-12-24T12:00:00Z"));
    private static final Timestamp DIEZ = Timestamp.from(Instant.parse("2026-12-24T13:00:00Z"));

    @Autowired
    protected JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private long profesional;
    private long sillon;
    private long usuario;

    @BeforeEach
    void crearRecursos() {
        String email = "prof-" + sufijo + "@example.com";
        jdbc.update("INSERT INTO profesional (nombre, apellido, email) VALUES ('N', 'A', ?)", email);
        profesional = jdbc.queryForObject("SELECT id FROM profesional WHERE email = ?", Long.class, email);
        jdbc.update("INSERT INTO sillon (nombre) VALUES (?)", "S-" + sufijo);
        sillon = jdbc.queryForObject("SELECT id FROM sillon WHERE nombre = ?", Long.class, "S-" + sufijo);
        String admin = "admin-" + sufijo + "@example.com";
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES (?, 'x', 'administrador')", admin);
        usuario = jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, admin);
    }

    private void insertarHorario(Object profesionalId, Object dia, String inicio, String fin) {
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, ?, CAST(? AS TIME), CAST(? AS TIME))", profesionalId, dia, inicio, fin);
    }

    private void insertarBloqueo(String ambito, Long profesionalId, Long sillonId, Timestamp inicio,
            Timestamp fin, String motivo, Long modificadoPor) {
        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, detalle, "
                + "modificado_por, modificado_en) VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?)",
                ambito, profesionalId, sillonId, inicio, fin, motivo, modificadoPor, NUEVE);
    }

    private static void rechazadoPor(String restriccion, Runnable insercion) {
        assertThatThrownBy(insercion::run)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT)).contains(restriccion));
    }

    @Test
    void aceptaHorarioYBloqueosDeCadaAmbito() {
        insertarHorario(profesional, 1, "09:00", "13:00");
        insertarHorario(profesional, 7, "14:00", "18:00");
        insertarBloqueo("PROFESIONAL", profesional, null, NUEVE, DIEZ, "AUSENCIA", usuario);
        insertarBloqueo("SILLON", null, sillon, NUEVE, DIEZ, "OTRO", usuario);
        insertarBloqueo("CONSULTORIO", null, null, NUEVE, DIEZ, "FERIADO", usuario);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM horario_atencion WHERE profesional_id = ?",
                Long.class, profesional)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bloqueo WHERE modificado_por = ?",
                Long.class, usuario)).isEqualTo(3);
    }

    @Test
    void rechazaDiaFueraDeRango() {
        rechazadoPor("ck_horario_atencion_dia", () -> insertarHorario(profesional, 0, "09:00", "13:00"));
        rechazadoPor("ck_horario_atencion_dia", () -> insertarHorario(profesional, 8, "09:00", "13:00"));
    }

    @Test
    void rechazaHorarioConInicioNoAnteriorAlFin() {
        rechazadoPor("ck_horario_atencion_rango", () -> insertarHorario(profesional, 1, "13:00", "13:00"));
        rechazadoPor("ck_horario_atencion_rango", () -> insertarHorario(profesional, 1, "18:00", "09:00"));
    }

    @Test
    void rechazaHorarioDeProfesionalInexistente() {
        assertThatThrownBy(() -> insertarHorario(-1L, 1, "09:00", "13:00"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rechazaAmbitoYMotivoDesconocidos() {
        rechazadoPor("ck_bloqueo_ambito",
                () -> insertarBloqueo("OTRO", null, null, NUEVE, DIEZ, "FERIADO", usuario));
        rechazadoPor("ck_bloqueo_motivo",
                () -> insertarBloqueo("CONSULTORIO", null, null, NUEVE, DIEZ, "PARO", usuario));
    }

    @Test
    void rechazaBloqueoConInicioNoAnteriorAlFin() {
        rechazadoPor("ck_bloqueo_rango",
                () -> insertarBloqueo("CONSULTORIO", null, null, DIEZ, DIEZ, "FERIADO", usuario));
        rechazadoPor("ck_bloqueo_rango",
                () -> insertarBloqueo("CONSULTORIO", null, null, DIEZ, NUEVE, "FERIADO", usuario));
    }

    @Test
    void rechazaRecursoIncoherenteConElAmbito() {
        String ck = "ck_bloqueo_recurso_segun_ambito";
        rechazadoPor(ck, () -> insertarBloqueo("PROFESIONAL", null, null, NUEVE, DIEZ, "AUSENCIA", usuario));
        rechazadoPor(ck, () -> insertarBloqueo("PROFESIONAL", profesional, sillon, NUEVE, DIEZ, "AUSENCIA", usuario));
        rechazadoPor(ck, () -> insertarBloqueo("SILLON", profesional, null, NUEVE, DIEZ, "OTRO", usuario));
        rechazadoPor(ck, () -> insertarBloqueo("CONSULTORIO", profesional, null, NUEVE, DIEZ, "FERIADO", usuario));
        rechazadoPor(ck, () -> insertarBloqueo("CONSULTORIO", null, sillon, NUEVE, DIEZ, "FERIADO", usuario));
    }

    @Test
    void rechazaBloqueoSinAutorOConRecursoInexistente() {
        assertThatThrownBy(() -> insertarBloqueo("CONSULTORIO", null, null, NUEVE, DIEZ, "FERIADO", null))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarBloqueo("PROFESIONAL", -1L, null, NUEVE, DIEZ, "AUSENCIA", usuario))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertarBloqueo("CONSULTORIO", null, null, NUEVE, DIEZ, "FERIADO", -1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

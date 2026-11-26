package ar.dentaflow.escenarios;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Fixture de pruebas (design D11): inserta por JDBC profesionales, sillones, prestaciones, pacientes, usuarios y
 * turnos ficticios (datos inventados, {@code @example.com}). Lo usan los tests de C-08 y C-09 para tener turnos
 * reales que cumplan la FK {@code fk_turno_historial_turno}. Cada creación usa valores únicos para convivir con
 * otras pruebas sobre la misma base.
 */
public final class EscenarioDeAgendaJdbc {

    private final JdbcTemplate jdbc;
    private final List<Long> usuarios = new ArrayList<>();
    private final List<Long> profesionales = new ArrayList<>();
    private final List<Long> sillones = new ArrayList<>();
    private final List<Long> prestaciones = new ArrayList<>();
    private final List<Long> pacientes = new ArrayList<>();
    private final List<Long> turnos = new ArrayList<>();

    public EscenarioDeAgendaJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String unico() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public long usuario() {
        String email = "agenda-" + unico() + "@example.com";
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES (?, '$2a$10$hashDePrueba', 'recepcion')",
                email);
        return registrar(usuarios, jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email));
    }

    public long profesional() {
        String email = "prof-" + unico() + "@example.com";
        jdbc.update("INSERT INTO profesional (nombre, apellido, matricula, email) VALUES ('Laura', 'Ejemplo', NULL, ?)",
                email);
        return registrar(profesionales,
                jdbc.queryForObject("SELECT id FROM profesional WHERE email = ?", Long.class, email));
    }

    public long sillon() {
        String nombre = "Sillon-" + unico();
        jdbc.update("INSERT INTO sillon (nombre) VALUES (?)", nombre);
        return registrar(sillones, jdbc.queryForObject("SELECT id FROM sillon WHERE nombre = ?", Long.class, nombre));
    }

    public long prestacion(int duracionMinutos) {
        String nombre = "Prestacion-" + unico();
        jdbc.update("INSERT INTO prestacion (nombre, duracion_minutos) VALUES (?, ?)", nombre, duracionMinutos);
        return registrar(prestaciones,
                jdbc.queryForObject("SELECT id FROM prestacion WHERE nombre = ?", Long.class, nombre));
    }

    public void habilitar(long profesionalId, long prestacionId) {
        jdbc.update("INSERT INTO profesional_prestacion (profesional_id, prestacion_id) VALUES (?, ?)",
                profesionalId, prestacionId);
    }

    /** Agrega horario 00:00–23:59 los 7 días de la semana al profesional dado. */
    public void atiendeTodaLaSemana(long profesionalId) {
        for (int dia = 1; dia <= 7; dia++) {
            jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                    + "VALUES (?, ?, '00:00:00', '23:59:00')", profesionalId, dia);
        }
    }

    /** DNI ficticio de 8 dígitos que no existe todavía en la base. */
    public String dniNuevo() {
        while (true) {
            String dni = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L));
            Long existentes = jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni = ?", Long.class, dni);
            if (existentes != null && existentes == 0) {
                return dni;
            }
        }
    }

    public long paciente() {
        String dni = dniNuevo();
        jdbc.update("INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en) "
                + "VALUES (?, 'Ana', 'Ejemplo', '1155550000', ?, ?)",
                dni, "pac-" + unico() + "@example.com", Timestamp.from(Instant.parse("2026-11-01T12:00:00Z")));
        return registrar(pacientes, jdbc.queryForObject("SELECT id FROM paciente WHERE dni = ?", Long.class, dni));
    }

    public String tokenNuevo() {
        byte[] bytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Inserta un turno con los datos dados (sin pasar por el servicio) y devuelve su id. */
    public long turno(long pacienteId, long profesionalId, long sillonId, long prestacionId, Instant inicio,
            Instant fin, String estado, String origen, boolean sobreturno, Long creadoPorUsuarioId, String token) {
        jdbc.update("INSERT INTO turno (paciente_id, profesional_id, sillon_id, prestacion_id, inicio, fin, estado, "
                + "origen, es_sobreturno, creado_por_usuario_id, creado_en, token_acceso) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                pacienteId, profesionalId, sillonId, prestacionId, Timestamp.from(inicio), Timestamp.from(fin),
                estado, origen, sobreturno, creadoPorUsuarioId, Timestamp.from(Instant.parse("2026-11-01T12:00:00Z")),
                token);
        return registrar(turnos, jdbc.queryForObject("SELECT id FROM turno WHERE token_acceso = ?", Long.class, token));
    }

    /** Un turno {@code pendiente} de 30 minutos, de recepción, con recursos y paciente nuevos. */
    public long turnoValido() {
        long usuario = usuario();
        return turno(paciente(), profesional(), sillon(), prestacion(30), Instant.parse("2026-12-01T12:00:00Z"),
                Instant.parse("2026-12-01T12:30:00Z"), "pendiente", "recepcion", false, usuario, tokenNuevo());
    }

    private static long registrar(List<Long> lista, Long id) {
        lista.add(id);
        return id;
    }

    /**
     * Para pruebas que hacen commit (sin {@code @Transactional}): borra, en orden de dependencias, todo lo creado por
     * este escenario y los turnos, y el historial, que el servicio haya creado sobre sus profesionales y sillones.
     * Así no deja filas que rompan pruebas que suponen las tablas de recursos vacías.
     */
    public void limpiar() {
        List<Long> deTurnos = new ArrayList<>(turnos);
        for (Long profesional : profesionales) {
            deTurnos.addAll(jdbc.queryForList("SELECT id FROM turno WHERE profesional_id = ?", Long.class, profesional));
        }
        for (Long sillon : sillones) {
            deTurnos.addAll(jdbc.queryForList("SELECT id FROM turno WHERE sillon_id = ?", Long.class, sillon));
        }
        deTurnos.forEach(id -> jdbc.update("DELETE FROM turno_historial WHERE turno_id = ?", id));
        deTurnos.forEach(id -> jdbc.update("DELETE FROM notificacion WHERE turno_id = ?", id));
        deTurnos.forEach(id -> jdbc.update("DELETE FROM turno WHERE id = ?", id));
        profesionales.forEach(id -> jdbc.update("DELETE FROM bloqueo WHERE profesional_id = ?", id));
        sillones.forEach(id -> jdbc.update("DELETE FROM bloqueo WHERE sillon_id = ?", id));
        profesionales.forEach(id -> jdbc.update("DELETE FROM horario_atencion WHERE profesional_id = ?", id));
        profesionales.forEach(id -> jdbc.update("DELETE FROM profesional_prestacion WHERE profesional_id = ?", id));
        profesionales.forEach(id -> jdbc.update("DELETE FROM profesional WHERE id = ?", id));
        sillones.forEach(id -> jdbc.update("DELETE FROM sillon WHERE id = ?", id));
        prestaciones.forEach(id -> jdbc.update("DELETE FROM prestacion WHERE id = ?", id));
        pacientes.forEach(id -> jdbc.update("DELETE FROM paciente WHERE id = ?", id));
        usuarios.forEach(id -> jdbc.update("DELETE FROM usuario WHERE id = ?", id));
    }
}

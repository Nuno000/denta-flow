package ar.dentaflow.infraestructura.persistencia.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * Spec auditoria-turnos ("Actor identificado", "Acción de un catálogo cerrado", "Valores anterior y
 * nuevo legibles") — restricciones del esquema {@code V008_1}, insertando por JDBC para probar la base
 * sin la aplicación. Las subclases eligen el motor. No asume tablas vacías y revierte todo al terminar.
 */
@Transactional
abstract class RestriccionesHistorialBase {

    private static final Timestamp AHORA = Timestamp.from(Instant.parse("2026-12-01T13:15:00Z"));
    private static final String ANTERIOR = "{\"estado\":\"pendiente\"}";
    private static final String NUEVO = "{\"estado\":\"confirmado\"}";

    @Autowired
    protected JdbcTemplate jdbc;

    private long insertarUsuario() {
        String email = "auditoria-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES (?, '$2a$10$hashDePrueba', 'recepcion')",
                email);
        return jdbc.queryForObject("SELECT id FROM usuario WHERE email = ?", Long.class, email);
    }

    private void insertar(String actorTipo, Long usuarioId, String accion, String anterior, String nuevo) {
        jdbc.update("INSERT INTO turno_historial (turno_id, actor_tipo, usuario_id, ocurrido_en, accion, "
                + "valor_anterior, valor_nuevo) VALUES (?, ?, ?, ?, ?, ?, ?)",
                42L, actorTipo, usuarioId, AHORA, accion, anterior, nuevo);
    }

    @Test
    void aceptaUnRegistroValidoDeCadaTipoDeActor() {
        long usuario = insertarUsuario();
        long antes = jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial", Long.class);

        insertar("usuario", usuario, "cambio_estado", ANTERIOR, NUEVO);
        insertar("paciente", null, "cancelacion", ANTERIOR, "{\"estado\":\"cancelado\"}");
        insertar("sistema", null, "creacion", null, NUEVO);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial", Long.class)).isEqualTo(antes + 3);
    }

    @Test
    void elTurnoNoNecesitaExistirPorqueLaFkLaAgregaC09() {
        insertar("sistema", null, "creacion", null, NUEVO);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id = 42", Long.class))
                .isPositive();
    }

    @Test
    void rechazaUnTipoDeActorDesconocido() {
        assertRechazadoPor("ck_turno_historial_actor_tipo",
                () -> insertar("robot", null, "creacion", null, NUEVO));
    }

    @Test
    void rechazaActorUsuarioSinId() {
        assertRechazadoPor("ck_turno_historial_actor_usuario",
                () -> insertar("usuario", null, "cambio_estado", ANTERIOR, NUEVO));
    }

    @Test
    void rechazaPacienteOSistemaConIdDeUsuario() {
        long usuario = insertarUsuario();

        assertRechazadoPor("ck_turno_historial_actor_usuario",
                () -> insertar("paciente", usuario, "cancelacion", ANTERIOR, NUEVO));
        assertRechazadoPor("ck_turno_historial_actor_usuario",
                () -> insertar("sistema", usuario, "cambio_estado", ANTERIOR, NUEVO));
    }

    @Test
    void rechazaUnaAccionFueraDelCatalogo() {
        assertRechazadoPor("ck_turno_historial_accion",
                () -> insertar("sistema", null, "borrado", ANTERIOR, NUEVO));
    }

    @Test
    void rechazaUnRegistroSinValores() {
        assertRechazadoPor("ck_turno_historial_valores",
                () -> insertar("sistema", null, "cambio_estado", null, null));
    }

    @Test
    void rechazaUnUsuarioInexistente() {
        assertRechazadoPor("fk_turno_historial_usuario",
                () -> insertar("usuario", 999_999_999L, "cambio_estado", ANTERIOR, NUEVO));
    }

    /**
     * Como en C-03 y C-04: H2 y MySQL traducen distinto cada violación (MySQL deja el CHECK como
     * {@code UncategorizedSQLException}). Se exige el rechazo nombrando la restricción.
     */
    private void assertRechazadoPor(String restriccion, ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT)).contains(restriccion));
    }
}

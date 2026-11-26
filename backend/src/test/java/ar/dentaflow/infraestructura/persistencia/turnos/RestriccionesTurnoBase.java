package ar.dentaflow.infraestructura.persistencia.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
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
 * Spec turnos — "Integridad de turnos en la base": restricciones del esquema {@code V009_1} insertando por JDBC.
 * Las subclases eligen el motor. Revierte todo al terminar.
 */
@Transactional
abstract class RestriccionesTurnoBase {

    private static final Instant INICIO = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant FIN = Instant.parse("2026-12-01T12:30:00Z");

    @Autowired
    protected JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profesional;
    private long sillon;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void crearRecursos() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        usuario = escenario.usuario();
    }

    private long insertar(long pacienteId, long profesionalId, long sillonId, long prestacionId, Instant inicio,
            Instant fin, String estado, String origen, boolean sobreturno, Long creador, String token) {
        return escenario.turno(pacienteId, profesionalId, sillonId, prestacionId, inicio, fin, estado, origen,
                sobreturno, creador, token);
    }

    private long insertarDeRecepcion(String estado, Instant inicio, Instant fin) {
        return insertar(paciente, profesional, sillon, prestacion, inicio, fin, estado, "recepcion", false, usuario,
                escenario.tokenNuevo());
    }

    @Test
    void aceptaUnTurnoValidoDeRecepcionYUnoOnline() {
        long deRecepcion = insertarDeRecepcion("pendiente", INICIO, FIN);
        long online = insertar(paciente, profesional, sillon, prestacion, INICIO, FIN, "confirmado", "online",
                false, null, escenario.tokenNuevo());

        assertThat(deRecepcion).isNotEqualTo(online);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE id IN (?, ?)", Long.class, deRecepcion,
                online)).isEqualTo(2L);
    }

    @Test
    void aceptaLosSeisEstadosDelCatalogo() {
        for (String estado : new String[] {"pendiente", "confirmado", "cancelado", "asistio", "ausente",
                "ausente_con_aviso"}) {
            insertarDeRecepcion(estado, INICIO, FIN);
        }

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE paciente_id = ?", Long.class, paciente))
                .isEqualTo(6L);
    }

    @Test
    void aceptaUnSobreturnoDeRecepcion() {
        long id = insertar(paciente, profesional, sillon, prestacion, INICIO, FIN, "pendiente", "recepcion", true,
                usuario, escenario.tokenNuevo());

        assertThat(jdbc.queryForObject("SELECT es_sobreturno FROM turno WHERE id = ?", Boolean.class, id)).isTrue();
    }

    @Test
    void rechazaUnEstadoFueraDelCatalogo() {
        assertRechazadoPor("ck_turno_estado", () -> insertarDeRecepcion("borrado", INICIO, FIN));
    }

    @Test
    void rechazaUnOrigenDesconocido() {
        assertRechazadoPor("ck_turno_origen", () -> insertar(paciente, profesional, sillon, prestacion, INICIO, FIN,
                "pendiente", "telefono", false, usuario, escenario.tokenNuevo()));
    }

    @Test
    void rechazaUnFinIgualOAnteriorAlInicio() {
        assertRechazadoPor("ck_turno_rango", () -> insertarDeRecepcion("pendiente", INICIO, INICIO));
        assertRechazadoPor("ck_turno_rango",
                () -> insertarDeRecepcion("pendiente", INICIO, INICIO.minusSeconds(60)));
    }

    @Test
    void rechazaUnSobreturnoOnline() {
        assertRechazadoPor("ck_turno_sobreturno_recepcion", () -> insertar(paciente, profesional, sillon, prestacion,
                INICIO, FIN, "pendiente", "online", true, null, escenario.tokenNuevo()));
    }

    @Test
    void rechazaUnOnlineConCreadorYUnaRecepcionSinCreador() {
        assertRechazadoPor("ck_turno_origen_creador", () -> insertar(paciente, profesional, sillon, prestacion,
                INICIO, FIN, "pendiente", "online", false, usuario, escenario.tokenNuevo()));
        assertRechazadoPor("ck_turno_origen_creador", () -> insertar(paciente, profesional, sillon, prestacion,
                INICIO, FIN, "pendiente", "recepcion", false, null, escenario.tokenNuevo()));
    }

    @Test
    void rechazaUnTokenRepetido() {
        String token = escenario.tokenNuevo();
        insertar(paciente, profesional, sillon, prestacion, INICIO, FIN, "pendiente", "recepcion", false, usuario,
                token);

        assertRechazadoPor("uk_turno_token_acceso", () -> insertar(paciente, profesional, sillon, prestacion,
                INICIO.plusSeconds(3600), FIN.plusSeconds(3600), "pendiente", "recepcion", false, usuario, token));
    }

    @Test
    void rechazaCadaReferenciaInexistente() {
        long inexistente = 999_999_999L;
        String token = escenario.tokenNuevo();

        assertRechazadoPor("fk_turno_paciente", () -> insertar(inexistente, profesional, sillon, prestacion, INICIO,
                FIN, "pendiente", "recepcion", false, usuario, token));
        assertRechazadoPor("fk_turno_profesional", () -> insertar(paciente, inexistente, sillon, prestacion, INICIO,
                FIN, "pendiente", "recepcion", false, usuario, token));
        assertRechazadoPor("fk_turno_sillon", () -> insertar(paciente, profesional, inexistente, prestacion, INICIO,
                FIN, "pendiente", "recepcion", false, usuario, token));
        assertRechazadoPor("fk_turno_prestacion", () -> insertar(paciente, profesional, sillon, inexistente, INICIO,
                FIN, "pendiente", "recepcion", false, usuario, token));
        assertRechazadoPor("fk_turno_creado_por", () -> insertar(paciente, profesional, sillon, prestacion, INICIO,
                FIN, "pendiente", "recepcion", false, inexistente, token));
    }

    /** H2 y MySQL traducen distinto cada violación: se exige el rechazo nombrando la restricción. */
    private void assertRechazadoPor(String restriccion, ThrowingCallable insercion) {
        assertThatThrownBy(insercion)
                .isInstanceOf(DataAccessException.class)
                .satisfies(e -> assertThat(e.getMessage().toLowerCase(Locale.ROOT)).contains(restriccion));
    }
}

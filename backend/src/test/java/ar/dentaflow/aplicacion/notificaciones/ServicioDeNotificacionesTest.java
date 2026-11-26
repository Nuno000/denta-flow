package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.email.EnviadorDeEmailSimulado;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spec notificaciones-email contra H2 con el canal simulado, el reloj fijo, el ejecutor síncrono y sin espera entre
 * intentos (perfil {@code test}, design D5 y D8). Sin {@code @Transactional}: cada paso confirma su transacción.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicioDeNotificacionesTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");
    private static final Instant INICIO = Instant.parse("2026-12-02T13:00:00Z");

    /** Envuelve al canal simulado y anota si había una transacción activa en cada envío. */
    static class EnviadorQueObservaTransacciones implements EnviadorDeEmail {

        final EnviadorDeEmailSimulado simulado = new EnviadorDeEmailSimulado();
        final List<Boolean> conTransaccion = new ArrayList<>();

        @Override
        public synchronized void enviar(MensajeEmail mensaje) {
            conTransaccion.add(TransactionSynchronizationManager.isActualTransactionActive());
            simulado.enviar(mensaje);
        }
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Primary
        Clock relojFijoDePrueba() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }

        @Bean
        @Primary
        EnviadorQueObservaTransacciones enviadorObservado() {
            return new EnviadorQueObservaTransacciones();
        }
    }

    @Autowired
    private ServicioDeNotificaciones servicio;

    @Autowired
    private ServicioDeTurnos turnos;

    @Autowired
    private EnviadorQueObservaTransacciones enviador;

    @Autowired
    private TransactionTemplate transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long turno;

    @BeforeEach
    void crearTurno() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        long profesional = escenario.profesional();
        long sillon = escenario.sillon();
        long prestacion = escenario.prestacion(30);
        escenario.habilitar(profesional, prestacion);
        turno = turnos.crear(new SolicitudDeTurno(paciente, profesional, sillon, prestacion, INICIO,
                OrigenTurno.ONLINE, false, Actor.paciente())).turno().getId();
        enviador.simulado.limpiar();
        enviador.conTransaccion.clear();
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    private String emailDelPaciente() {
        return jdbc.queryForObject("SELECT email FROM paciente WHERE id = ?", String.class, paciente);
    }

    private Map<String, Object> notificacion() {
        return jdbc.queryForMap("SELECT estado, intentos, motivo_fallo, enviada_en, canal FROM notificacion "
                + "WHERE turno_id = ? AND tipo = 'confirmacion'", turno);
    }

    private long notificacionesDelTurno() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notificacion WHERE turno_id = ?", Long.class, turno);
    }

    @Test
    void enviaLaConfirmacionAlPacienteYLaRegistraEnviada() {
        servicio.enviarConfirmacion(turno);

        List<MensajeEmail> enviados = enviador.simulado.mensajes();
        assertThat(enviados).singleElement().satisfies(m -> {
            assertThat(m.destinatario()).isEqualTo(emailDelPaciente());
            assertThat(m.textoPlano()).contains("02/12/2026 10:00", "/turno/", "/confirmar", "Laura Ejemplo");
        });
        Map<String, Object> fila = notificacion();
        assertThat(fila).containsEntry("estado", "enviada").containsEntry("canal", "email");
        assertThat(((Number) fila.get("intentos")).intValue()).isEqualTo(1);
        assertThat(((Timestamp) fila.get("enviada_en")).toInstant()).isEqualTo(AHORA);
    }

    @Test
    void losEnlacesDelEmailUsanElTokenDelTurno() {
        servicio.enviarConfirmacion(turno);

        String token = jdbc.queryForObject("SELECT token_acceso FROM turno WHERE id = ?", String.class, turno);
        assertThat(enviador.simulado.mensajes().get(0).textoPlano())
                .contains("http://localhost:5173/turno/" + token + "/confirmar",
                        "http://localhost:5173/turno/" + token + "/cancelar",
                        "http://localhost:5173/turno/" + token + "/reprogramar");
    }

    @Test
    void pedirlaDosVecesEnviaUnSoloEmail() {
        servicio.enviarConfirmacion(turno);
        servicio.enviarConfirmacion(turno);

        assertThat(enviador.simulado.mensajes()).hasSize(1);
        assertThat(notificacionesDelTurno()).isEqualTo(1L);
    }

    @Test
    void siFallaUnaVezPorConexionReintentaYQuedaEnviadaConDosIntentos() {
        enviador.simulado.fallarLasProximas(1, MotivoFallo.CONEXION);

        servicio.enviarConfirmacion(turno);

        assertThat(notificacion()).containsEntry("estado", "enviada");
        assertThat(((Number) notificacion().get("intentos")).intValue()).isEqualTo(2);
        assertThat(enviador.simulado.mensajes()).hasSize(1);
    }

    @Test
    void siFallaSiempreHaceTresIntentosYQuedaFallidaPorConexion() {
        enviador.simulado.fallarLasProximas(10, MotivoFallo.CONEXION);

        servicio.enviarConfirmacion(turno);

        assertThat(enviador.simulado.intentos()).isEqualTo(3);
        assertThat(notificacion()).containsEntry("estado", "fallida").containsEntry("motivo_fallo", "conexion");
        assertThat(((Number) notificacion().get("intentos")).intValue()).isEqualTo(3);
        assertThat(notificacion().get("enviada_en")).isNull();
    }

    @Test
    void conCredencialesInvalidasHaceUnSoloIntento() {
        enviador.simulado.fallarLasProximas(10, MotivoFallo.AUTENTICACION);

        servicio.enviarConfirmacion(turno);

        assertThat(enviador.simulado.intentos()).isEqualTo(1);
        assertThat(notificacion()).containsEntry("estado", "fallida").containsEntry("motivo_fallo", "autenticacion");
    }

    @Test
    void conElServidorCaidoElTurnoSigueIgualYElEnvioNoCorreDentroDeUnaTransaccion() {
        enviador.simulado.fallarLasProximas(10, MotivoFallo.CONEXION);

        servicio.enviarConfirmacion(turno);

        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turno))
                .isEqualTo("pendiente");
        assertThat(enviador.conTransaccion).hasSize(3).containsOnly(false);
    }

    @Test
    void pedidaDentroDeUnaTransaccionSeEnviaRecienDespuesDelCommit() {
        transaccion.executeWithoutResult(estado -> {
            servicio.enviarConfirmacion(turno);
            assertThat(enviador.simulado.mensajes()).isEmpty();
        });

        assertThat(enviador.simulado.mensajes()).hasSize(1);
        assertThat(enviador.conTransaccion).containsOnly(false);
    }

    @Test
    void siLaTransaccionSeRevierteNoSeEnviaNada() {
        transaccion.executeWithoutResult(estado -> {
            servicio.enviarConfirmacion(turno);
            estado.setRollbackOnly();
        });

        assertThat(enviador.simulado.mensajes()).isEmpty();
        assertThat(notificacionesDelTurno()).isZero();
    }

    @Test
    void aUnPacienteAnonimizadoNoLeEnviaYQuedaOmitida() {
        long usuario = escenario.usuario();
        jdbc.update("UPDATE paciente SET dni = NULL, anonimizado_en = ?, anonimizado_por = ? WHERE id = ?",
                Timestamp.from(AHORA), usuario, paciente);

        servicio.enviarConfirmacion(turno);

        assertThat(enviador.simulado.intentos()).isZero();
        assertThat(notificacion()).containsEntry("estado", "omitida");
        assertThat(((Number) notificacion().get("intentos")).intValue()).isZero();
    }
}

package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.email.EnviadorDeEmailSimulado;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Spec notificaciones-email — "Sin datos personales ni enlaces en los logs" (design D5). */
@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LogsDeNotificacionesTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijoDePrueba() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private ServicioDeNotificaciones servicio;

    @Autowired
    private ServicioDeTurnos turnos;

    @Autowired
    private EnviadorDeEmailSimulado enviador;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long turno;
    private String token;

    @BeforeEach
    void crearTurno() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        long profesional = escenario.profesional();
        long sillon = escenario.sillon();
        long prestacion = escenario.prestacion(30);
        escenario.habilitar(profesional, prestacion);
        turno = turnos.crear(new SolicitudDeTurno(paciente, profesional, sillon, prestacion,
                Instant.parse("2026-12-02T13:00:00Z"), OrigenTurno.ONLINE, false, Actor.paciente())).turno().getId();
        token = jdbc.queryForObject("SELECT token_acceso FROM turno WHERE id = ?", String.class, turno);
        enviador.limpiar();
    }

    @AfterEach
    void limpiar() {
        enviador.limpiar();
        escenario.limpiar();
    }

    private void assertSinDatosPersonales(CapturedOutput salida) {
        String email = jdbc.queryForObject("SELECT email FROM paciente WHERE id = ?", String.class, paciente);
        assertThat(salida.getAll()).doesNotContain(email).doesNotContain(token).doesNotContain("/turno/")
                .doesNotContain("Ana Ejemplo");
    }

    @Test
    void unEnvioExitosoSoloDejaIdsEstadoEIntentos(CapturedOutput salida) {
        servicio.enviarConfirmacion(turno);

        assertThat(salida.getAll()).contains("turno=" + turno).contains("estado=enviada").contains("intentos=1");
        assertSinDatosPersonales(salida);
    }

    @Test
    void unEnvioFallidoDejaElMotivoSinDatosPersonales(CapturedOutput salida) {
        enviador.fallarLasProximas(10, MotivoFallo.DESTINATARIO);

        servicio.enviarConfirmacion(turno);

        assertThat(salida.getAll()).contains("turno=" + turno).contains("estado=fallida")
                .contains("motivo=destinatario").containsPattern("notificacion=\\d+");
        assertSinDatosPersonales(salida);
    }
}

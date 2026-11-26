package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.notificaciones.SituacionDeEnlace;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.lang.reflect.RecordComponent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
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

/**
 * Spec enlaces-paciente — "Resolución de un token" y "La resolución no expone ni modifica el turno" (design D6),
 * contra H2 con el reloj fijo.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicioDeEnlacesTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");
    private static final Instant DOCE = Instant.parse("2026-12-01T12:00:00Z");

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijoDePrueba() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private ServicioDeEnlaces servicio;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profesional;
    private long sillon;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void crearEscenario() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        usuario = escenario.usuario();
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    private String turnoCon(String estado, Instant inicio) {
        String token = escenario.tokenNuevo();
        escenario.turno(paciente, profesional, sillon, prestacion, inicio, inicio.plusSeconds(1800), estado,
                "recepcion", false, usuario, token);
        return token;
    }

    @Test
    void unTurnoPendienteFuturoEsVigenteYTraeSuResumen() {
        String token = turnoCon("pendiente", DOCE);

        ResolucionDeEnlace resolucion = servicio.resolver(token);

        assertThat(resolucion.situacion()).isEqualTo(SituacionDeEnlace.VIGENTE);
        ResumenTurnoPaciente resumen = resolucion.resumen().orElseThrow();
        assertThat(resumen.inicio()).isEqualTo(DOCE);
        assertThat(resumen.fin()).isEqualTo(DOCE.plusSeconds(1800));
        assertThat(resumen.estado()).isEqualTo(EstadoTurno.PENDIENTE);
        assertThat(resumen.prestacion()).startsWith("Prestacion-");
        assertThat(resumen.profesional()).isEqualTo("Laura Ejemplo");
    }

    @Test
    void unTurnoCanceladoEsCancelado() {
        assertThat(servicio.resolver(turnoCon("cancelado", DOCE)).situacion()).isEqualTo(SituacionDeEnlace.CANCELADO);
    }

    @Test
    void unTurnoCuyoInicioYaLlegoEstaVencido() {
        assertThat(servicio.resolver(turnoCon("pendiente", AHORA)).situacion()).isEqualTo(SituacionDeEnlace.VENCIDO);
    }

    @Test
    void unTokenInexistenteOMalFormadoDaLaMismaRespuestaSinDatos() {
        List<String> tokens = Arrays.asList(escenario.tokenNuevo(), "../x", "", null, "a".repeat(44));

        for (String token : tokens) {
            ResolucionDeEnlace resolucion = servicio.resolver(token);
            assertThat(resolucion.situacion()).isEqualTo(SituacionDeEnlace.INEXISTENTE);
            assertThat(resolucion.resumen()).isEmpty();
        }
    }

    @Test
    void resolverDosVecesNoCambiaElEstadoNiAgregaHistorial() {
        String token = turnoCon("pendiente", DOCE);

        servicio.resolver(token);
        servicio.resolver(token);

        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE token_acceso = ?", String.class, token))
                .isEqualTo("pendiente");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id = "
                + "(SELECT id FROM turno WHERE token_acceso = ?)", Long.class, token)).isZero();
    }

    @Test
    void elResumenNoTieneDatosDelPaciente() {
        List<String> campos = Arrays.stream(ResumenTurnoPaciente.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();

        assertThat(campos).containsExactly("inicio", "fin", "estado", "prestacion", "profesional");
        String texto = servicio.resolver(turnoCon("pendiente", DOCE)).toString();
        assertThat(texto).doesNotContain("pac-").doesNotContain("1155550000").doesNotContain("Ana");
    }
}

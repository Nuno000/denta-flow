package ar.dentaflow.presentacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec privacidad-datos-personales (C-15) sobre HTTP: "Anonimización del paciente", "Anonimización solo por el
 * administrador" y "Sin datos personales en logs ni en errores" (tareas 7.3, 7.4 y 8.1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PacienteAdminControllerTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");

    @TestConfiguration
    static class RelojFijo {
        @Bean
        @Primary
        Clock relojFijo() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    private EscenarioDeAgendaJdbc escenario;
    private String admin;
    private String recepcion;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        admin = token(Rol.ADMINISTRADOR);
        recepcion = token(Rol.RECEPCION);
        logs.start();
        raiz.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        raiz.detachAppender(logs);
    }

    private String token(Rol rol) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        return "Bearer " + emisor.emitir(usuarios.crear(email, "contrasena-valida", rol, null).usuario().getId()).token();
    }

    private String anonimizar(long pacienteId) {
        return "/api/pacientes/" + pacienteId + "/anonimizar";
    }

    /** Lee por JDBC después de bajar a la base lo que JPA tiene pendiente en la transacción del test. */
    private Map<String, Object> fila(long pacienteId) {
        entityManager.flush();
        return jdbc.queryForMap("SELECT dni, nombre, email, anonimizado_en FROM paciente WHERE id = ?", pacienteId);
    }

    private long turnoDe(long pacienteId, Instant inicio, String estado) {
        long profesional = escenario.profesional();
        long prestacion = escenario.prestacion(30);
        escenario.habilitar(profesional, prestacion);
        return escenario.turno(pacienteId, profesional, escenario.sillon(), prestacion, inicio,
                inicio.plusSeconds(1800), estado, "online", false, null, escenario.tokenNuevo());
    }

    @Test
    void elAdministradorAnonimizaYLosTurnosPasadosQuedan() throws Exception {
        long paciente = escenario.paciente();
        String dni = (String) fila(paciente).get("dni");
        long turnoPasado = turnoDe(paciente, AHORA.minusSeconds(86_400), "asistio");

        mockMvc.perform(post(anonimizar(paciente)).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paciente))
                .andExpect(jsonPath("$.anonimizadoEn").value("2026-12-01T12:00:00Z"));

        Map<String, Object> despues = fila(paciente);
        assertThat(despues.get("dni")).isNull();
        assertThat(despues.get("nombre")).isEqualTo("Paciente");
        assertThat((String) despues.get("email")).endsWith("@dentaflow.invalid");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE id = ?", Long.class, turnoPasado)).isEqualTo(1L);
        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(m -> m.contains("Paciente " + paciente + " anonimizado"))
                .noneMatch(m -> m.contains(dni) || m.contains("Ejemplo"));
    }

    @Test
    void anonimizarDosVecesDevuelveLoMismo() throws Exception {
        long paciente = escenario.paciente();
        mockMvc.perform(post(anonimizar(paciente)).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk());

        mockMvc.perform(post(anonimizar(paciente)).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anonimizadoEn").value("2026-12-01T12:00:00Z"));
    }

    @Test
    void conUnTurnoFuturoActivoEsConflictoYNoCambiaNada() throws Exception {
        long paciente = escenario.paciente();
        String dni = (String) fila(paciente).get("dni");
        turnoDe(paciente, AHORA.plusSeconds(86_400), "confirmado");

        String cuerpo = mockMvc.perform(post(anonimizar(paciente)).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).contains("cancelalos").doesNotContain(dni);
        assertThat(fila(paciente).get("dni")).isEqualTo(dni);
    }

    @Test
    void unTurnoFuturoCanceladoNoImpideLaBaja() throws Exception {
        long paciente = escenario.paciente();
        turnoDe(paciente, AHORA.plusSeconds(86_400), "cancelado");

        mockMvc.perform(post(anonimizar(paciente)).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk());
    }

    @Test
    void pacienteInexistenteEs404() throws Exception {
        mockMvc.perform(post(anonimizar(999_999L)).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void recepcionRecibe403YSinTokenRecibe401SinCambios() throws Exception {
        long paciente = escenario.paciente();
        String dni = (String) fila(paciente).get("dni");

        mockMvc.perform(post(anonimizar(paciente)).header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(anonimizar(paciente))).andExpect(status().isUnauthorized());

        assertThat(fila(paciente).get("dni")).isEqualTo(dni);
    }
}

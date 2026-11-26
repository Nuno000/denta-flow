package ar.dentaflow.presentacion.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.notificaciones.EnviadorDeEmail;
import ar.dentaflow.dominio.notificaciones.MensajeEmail;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.email.EnviadorDeEmailSimulado;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tareas 5.3, 5.4 y 5.5: Tests de integración de la reserva pública con H2, MockMvc y canal simulado de email.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservasPublicasControllerTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final Instant AHORA = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(LUNES, LocalTime.of(8, 0)));

    @TestConfiguration
    static class RelojFijoConfig {
        @Bean
        @Primary
        Clock relojFijoReservaPublica() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EnviadorDeEmail enviadorDeEmail;

    private EscenarioDeAgendaJdbc escenario;
    private long profA;
    private long sillon;
    private long prestacion;

    @BeforeEach
    void crearEscenario() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        profA = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(profA, prestacion);
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 1, '09:00:00', '12:00:00')", profA);

        if (enviadorDeEmail instanceof EnviadorDeEmailSimulado simulado) {
            simulado.limpiar();
        }
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
        if (enviadorDeEmail instanceof EnviadorDeEmailSimulado simulado) {
            simulado.limpiar();
        }
    }

    private String jsonReserva(String dni, String email, Boolean aceptaAviso, String inicio, Long profesionalId) {
        String profCampo = profesionalId != null ? "\"profesionalId\": " + profesionalId + "," : "";
        String avisoCampo = aceptaAviso != null ? "\"aceptaAvisoPrivacidad\": " + aceptaAviso : "";
        return """
                {
                  "prestacionId": %d,
                  %s
                  "inicio": "%s",
                  "paciente": {
                    "dni": "%s",
                    "nombre": "Ana",
                    "apellido": "García",
                    "telefono": "0261 555-1234",
                    "email": "%s"
                  }%s
                }
                """.formatted(prestacion, profCampo, inicio, dni, email, avisoCampo.isEmpty() ? "" : ", " + avisoCampo);
    }

    @Test
    void reservaCualquieraValidaCreaTurnoPendienteYEnviaEmail() throws Exception {
        String dni = escenario.dniNuevo();
        String json = jsonReserva(dni, "ana.nueva@example.com", true, "2026-11-30T09:00:00-03:00", null);

        mockMvc.perform(post("/api/publico/reservas")
                        .with(req -> {
                            req.setRemoteAddr("203.0.113.11");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.inicio", endsWith("-03:00")))
                .andExpect(jsonPath("$.fin", endsWith("-03:00")))
                .andExpect(jsonPath("$.prestacion", notNullValue()))
                .andExpect(jsonPath("$.profesional", notNullValue()))
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.tokenAcceso").doesNotExist())
                .andExpect(jsonPath("$.sillonId").doesNotExist())
                .andExpect(jsonPath("$.paciente").doesNotExist())
                .andExpect(jsonPath("$.dni").doesNotExist());

        // Verificación en BD: turno online, no sobreturno, sillón asignado, con historial
        Long turnoId = jdbc.queryForObject(
                "SELECT t.id FROM turno t JOIN paciente p ON t.paciente_id = p.id WHERE p.dni = ?",
                Long.class, dni);
        assertThat(turnoId).isNotNull();

        String origen = jdbc.queryForObject("SELECT origen FROM turno WHERE id = ?", String.class, turnoId);
        Boolean sobreturno = jdbc.queryForObject("SELECT es_sobreturno FROM turno WHERE id = ?", Boolean.class, turnoId);
        Long sillonId = jdbc.queryForObject("SELECT sillon_id FROM turno WHERE id = ?", Long.class, turnoId);
        assertThat(origen).isEqualTo("online");
        assertThat(sobreturno).isFalse();
        assertThat(sillonId).isEqualTo(sillon);

        Integer historialCount = jdbc.queryForObject(
                "SELECT count(*) FROM turno_historial WHERE turno_id = ?", Integer.class, turnoId);
        assertThat(historialCount).isGreaterThanOrEqualTo(1);

        // Canal simulado tiene un email con enlaces
        if (enviadorDeEmail instanceof EnviadorDeEmailSimulado simulado) {
            List<MensajeEmail> emails = simulado.mensajesPara("ana.nueva@example.com");
            assertThat(emails).hasSize(1);
            assertThat(emails.get(0).textoPlano()).contains("/turno/");
            assertThat(emails.get(0).textoPlano()).contains("/confirmar");
            assertThat(emails.get(0).textoPlano()).contains("/cancelar");
        }
    }

    @Test
    void validacionesDeEntradaCamposInvalidosYExcepciones() throws Exception {
        String dni = escenario.dniNuevo();

        // 1. Email inválido ("ana@"): 400 por campo sin repetir el valor
        String jsonEmailInvalido = jsonReserva(dni, "ana@", true, "2026-11-30T09:00:00-03:00", profA);
        String cuerpoEmailInvalido = mockMvc.perform(post("/api/publico/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonEmailInvalido))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpoEmailInvalido).contains("email");
        assertThat(cuerpoEmailInvalido).doesNotContain("ana@");

        // 2. Aviso false / ausente: 400 con mensaje de C-15 y sin paciente ni turno
        String dniSinAviso = escenario.dniNuevo();
        String jsonSinAviso = jsonReserva(dniSinAviso, "sin.aviso@example.com", false, "2026-11-30T09:00:00-03:00", profA);
        mockMvc.perform(post("/api/publico/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSinAviso))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.aceptaAvisoPrivacidad", containsString(ServicioDePacientes.AVISO_NO_ACEPTADO)));

        Long pacienteExiste = jdbc.queryForObject("SELECT count(*) FROM paciente WHERE dni = ?", Long.class, dniSinAviso);
        assertThat(pacienteExiste).isEqualTo(0L);

        // 3. sillonId o esSobreturno inyectados en el cuerpo se ignoran
        String jsonInyeccion = """
                {
                  "prestacionId": %d,
                  "profesionalId": %d,
                  "sillonId": 9999,
                  "esSobreturno": true,
                  "inicio": "2026-11-30T09:30:00-03:00",
                  "paciente": {
                    "dni": "%s",
                    "nombre": "Ana",
                    "apellido": "García",
                    "telefono": "0261 555-1234",
                    "email": "inyeccion@example.com"
                  },
                  "aceptaAvisoPrivacidad": true
                }
                """.formatted(prestacion, profA, dni);
        mockMvc.perform(post("/api/publico/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInyeccion))
                .andExpect(status().isCreated());

        Long turnoInyectadoId = jdbc.queryForObject(
                "SELECT t.id FROM turno t JOIN paciente p ON t.paciente_id = p.id WHERE p.dni = ?",
                Long.class, dni);
        Boolean sobreturno = jdbc.queryForObject("SELECT es_sobreturno FROM turno WHERE id = ?", Boolean.class, turnoInyectadoId);
        Long sillonReal = jdbc.queryForObject("SELECT sillon_id FROM turno WHERE id = ?", Long.class, turnoInyectadoId);
        assertThat(sobreturno).isFalse();
        assertThat(sillonReal).isEqualTo(sillon);

        // 4. Horario ya ocupado: 409 con mensaje "El horario elegido ya no está disponible; elegí otro" y sin email
        if (enviadorDeEmail instanceof EnviadorDeEmailSimulado simulado) {
            simulado.limpiar();
        }
        String dniConflicto = escenario.dniNuevo();
        String jsonConflicto = jsonReserva(dniConflicto, "otro@example.com", true, "2026-11-30T09:30:00-03:00", profA);
        mockMvc.perform(post("/api/publico/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConflicto))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El horario elegido ya no está disponible; elegí otro"));

        if (enviadorDeEmail instanceof EnviadorDeEmailSimulado simulado) {
            assertThat(simulado.mensajesPara("otro@example.com")).isEmpty();
        }

        // 5. Paciente con DNI existente y otro email: reserva creada, email del paciente no se cambia
        String dniExistente = dni; // Creado en paso 3 con email "inyeccion@example.com"
        String jsonDniExistente = jsonReserva(dniExistente, "intento.cambio@example.com", true, "2026-11-30T10:00:00-03:00", profA);
        mockMvc.perform(post("/api/publico/reservas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDniExistente))
                .andExpect(status().isCreated());

        String emailEnBase = jdbc.queryForObject("SELECT email FROM paciente WHERE dni = ?", String.class, dniExistente);
        assertThat(emailEnBase).isEqualTo("inyeccion@example.com"); // No fue reemplazado por la reserva online
    }

    @Test
    void limiteDeCincoPedidosPorIpYElSextoDa429() throws Exception {
        String ip = "198.51.100.99";
        for (int i = 0; i < 5; i++) {
            String dni = escenario.dniNuevo();
            // Pedidos válidos o con horario fuera de rango, cuentan para el límite
            String json = jsonReserva(dni, "ana" + i + "@example.com", true, "2026-11-30T11:00:00-03:00", profA);
            mockMvc.perform(post("/api/publico/reservas")
                            .with(req -> {
                                req.setRemoteAddr(ip);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json));
        }

        // El 6º pedido desde la misma IP debe devolver 429 con Retry-After
        String dniSexto = escenario.dniNuevo();
        String jsonSexto = jsonReserva(dniSexto, "ana6@example.com", true, "2026-11-30T11:00:00-03:00", profA);
        mockMvc.perform(post("/api/publico/reservas")
                        .with(req -> {
                            req.setRemoteAddr(ip);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonSexto))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail", containsString("Hiciste demasiados pedidos de reserva")));

        // Y no se creó el paciente del 6º intento
        Long count = jdbc.queryForObject("SELECT count(*) FROM paciente WHERE dni = ?", Long.class, dniSexto);
        assertThat(count).isEqualTo(0L);
    }
}

package ar.dentaflow.presentacion.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tarea 5.6 (spec "Sin datos personales en logs ni errores de la reserva", design D7):
 * Asegura que una reserva exitosa y una rechazada (409) no dejen en el log ni en las respuestas
 * el DNI, nombre, apellido, teléfono, email ni la IP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LogsDeReservaPublicaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final Instant AHORA = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(LUNES, LocalTime.of(8, 0)));
    private static final String IP = "203.0.113.88";
    private static final String TELEFONO = "0261 555-9876";
    private static final String TELEFONO_NORM = "02615559876";

    @TestConfiguration
    static class RelojFijoConfig {
        @Bean
        @Primary
        Clock relojFijoLogs() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long prof;
    private long sillon;
    private long prestacion;

    @BeforeEach
    void crearEscenario() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        prof = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(prof, prestacion);
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 1, '09:00:00', '12:00:00')", prof);
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    @Test
    void reservaExitosaYRechazadaNoDejanDatosPersonalesEnLogsNiEnCuerpo(CapturedOutput salida) throws Exception {
        String dni = escenario.dniNuevo();
        String email = "paciente.privado@example.com";
        String json = """
                {
                  "prestacionId": %d,
                  "profesionalId": %d,
                  "inicio": "2026-11-30T09:00:00-03:00",
                  "paciente": {
                    "dni": "%s",
                    "nombre": "Florencia",
                    "apellido": "Silvestre",
                    "telefono": "%s",
                    "email": "%s"
                  },
                  "aceptaAvisoPrivacidad": true
                }
                """.formatted(prestacion, prof, dni, TELEFONO, email);

        // 1. Reserva exitosa (201)
        String respuesta201 = mockMvc.perform(post("/api/publico/reservas")
                        .with(req -> {
                            req.setRemoteAddr(IP);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta201).doesNotContain(dni)
                .doesNotContain("Florencia")
                .doesNotContain("Silvestre")
                .doesNotContain(TELEFONO)
                .doesNotContain(TELEFONO_NORM)
                .doesNotContain(email)
                .doesNotContain(IP);

        // 2. Reserva rechazada por hueco ocupado (409)
        String dni2 = escenario.dniNuevo();
        String email2 = "otro.privado@example.com";
        String jsonConflicto = """
                {
                  "prestacionId": %d,
                  "profesionalId": %d,
                  "inicio": "2026-11-30T09:00:00-03:00",
                  "paciente": {
                    "dni": "%s",
                    "nombre": "Ramiro",
                    "apellido": "Benitez",
                    "telefono": "0261 555-4321",
                    "email": "%s"
                  },
                  "aceptaAvisoPrivacidad": true
                }
                """.formatted(prestacion, prof, dni2, email2);

        String respuesta409 = mockMvc.perform(post("/api/publico/reservas")
                        .with(req -> {
                            req.setRemoteAddr(IP);
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonConflicto))
                .andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(respuesta409).doesNotContain(dni2)
                .doesNotContain("Ramiro")
                .doesNotContain("Benitez")
                .doesNotContain("0261 555-4321")
                .doesNotContain("02615554321")
                .doesNotContain(email2)
                .doesNotContain(IP);

        // 3. Verificación de logs capturados
        String logs = salida.getAll();
        assertThat(logs).doesNotContain(dni)
                .doesNotContain("Florencia")
                .doesNotContain("Silvestre")
                .doesNotContain(TELEFONO)
                .doesNotContain(TELEFONO_NORM)
                .doesNotContain(email)
                .doesNotContain(dni2)
                .doesNotContain("Ramiro")
                .doesNotContain("Benitez")
                .doesNotContain("0261 555-4321")
                .doesNotContain("02615554321")
                .doesNotContain(email2)
                .doesNotContain(IP);
    }
}

package ar.dentaflow.presentacion.publico;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
 * Spec motor-disponibilidad — "Consulta pública de huecos" (tareas 6.1 y 6.2, design D7).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HuecosPublicosControllerTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final Instant AHORA = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(LUNES, LocalTime.of(8, 0)));

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijoDePrueba() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

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
                + "VALUES (?, 1, '09:00:00', '10:00:00')", profA);
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    @Test
    void consultaSinTokenDevuelveHuecosOrdenadosEnZonaLocalYSinSillon() throws Exception {
        mockMvc.perform(get("/api/publico/huecos")
                        .param("prestacionId", String.valueOf(prestacion))
                        .param("desde", "2026-11-30")
                        .param("hasta", "2026-12-06"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].inicio", endsWith("-03:00")))
                .andExpect(jsonPath("$[0].fin", endsWith("-03:00")))
                .andExpect(jsonPath("$[0].profesionalId").value(nullValue()))
                .andExpect(jsonPath("$[0].sillonId").doesNotExist());
    }

    @Test
    void conProfesionalIdLoDevuelveEnCadaHuecoYSinHuecosDevuelveListaVacia() throws Exception {
        mockMvc.perform(get("/api/publico/huecos")
                        .param("prestacionId", String.valueOf(prestacion))
                        .param("profesionalId", String.valueOf(profA))
                        .param("desde", "2026-11-30")
                        .param("hasta", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].profesionalId").value(profA));

        mockMvc.perform(get("/api/publico/huecos")
                        .param("prestacionId", String.valueOf(prestacion))
                        .param("profesionalId", String.valueOf(profA))
                        .param("desde", "2026-12-06")
                        .param("hasta", "2026-12-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void erroresDevuelvenProblemJsonEnEspanolYOpenApiListaLaRuta() throws Exception {
        mockMvc.perform(get("/api/publico/huecos")
                        .param("desde", "2026-11-30")
                        .param("hasta", "2026-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/publico/huecos")
                        .param("prestacionId", String.valueOf(prestacion))
                        .param("desde", "30-11-2026")
                        .param("hasta", "2026-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/publico/huecos")
                        .param("prestacionId", String.valueOf(prestacion))
                        .param("desde", "2026-11-30")
                        .param("hasta", "2026-12-14"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/publico/huecos")
                        .param("prestacionId", "999999999")
                        .param("desde", "2026-11-30")
                        .param("hasta", "2026-12-01"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/publico/huecos")));
    }
}

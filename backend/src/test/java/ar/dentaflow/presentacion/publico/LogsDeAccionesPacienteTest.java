package ar.dentaflow.presentacion.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LogsDeAccionesPacienteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long prof;
    private long sillon;
    private long prestacion;
    private long paciente;
    private String token;
    private String dniPaciente;
    private String emailPaciente = "paciente-privado@example.com";
    private String telefonoPaciente = "1155559999";
    private String nombrePaciente = "PacienteSensible";

    @BeforeEach
    void setUp() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        prof = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(prof, prestacion);
        escenario.atiendeTodaLaSemana(prof);

        dniPaciente = escenario.dniNuevo();
        jdbc.update("INSERT INTO paciente (dni, nombre, apellido, telefono, email, creado_en) "
                + "VALUES (?, ?, 'Secreto', ?, ?, CURRENT_TIMESTAMP)",
                dniPaciente, nombrePaciente, telefonoPaciente, emailPaciente);
        paciente = jdbc.queryForObject("SELECT id FROM paciente WHERE dni = ?", Long.class, dniPaciente);

        token = escenario.tokenNuevo();
        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(10);
        Instant inicio = LocalDateTime.of(fecha, LocalTime.of(10, 0))
                .atZone(ZonaHorariaConsultorio.ZONA).toInstant();
        Instant fin = inicio.plusSeconds(1800);

        escenario.turno(paciente, prof, sillon, prestacion, inicio, fin,
                "pendiente", "recepcion", false, escenario.usuario(), token);
    }

    @AfterEach
    void tearDown() {
        escenario.limpiar();
    }

    @Test
    void confirmarCancelarYReprogramarNoDejanEnLogDatosPersonalesNiElToken(CapturedOutput output) throws Exception {
        // 1. Confirmar
        mockMvc.perform(post("/api/publico/turnos/" + token + "/confirmar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.11");
                            return req;
                        }))
                .andExpect(status().isOk());

        // 2. Reprogramar
        LocalDate fechaNueva = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(11);
        String nuevoInicioIso = LocalDateTime.of(fechaNueva, LocalTime.of(11, 0))
                .atZone(ZonaHorariaConsultorio.ZONA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        mockMvc.perform(post("/api/publico/turnos/" + token + "/reprogramar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"" + nuevoInicioIso + "\"}")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.12");
                            return req;
                        }))
                .andExpect(status().isOk());

        // 3. Cancelar
        mockMvc.perform(post("/api/publico/turnos/" + token + "/cancelar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.13");
                            return req;
                        }))
                .andExpect(status().isOk());

        String logs = output.getAll();

        // Verificar que no se loguean datos sensibles ni el token
        assertThat(logs).doesNotContain(token);
        assertThat(logs).doesNotContain(dniPaciente);
        assertThat(logs).doesNotContain(nombrePaciente);
        assertThat(logs).doesNotContain(emailPaciente);
        assertThat(logs).doesNotContain(telefonoPaciente);
    }
}

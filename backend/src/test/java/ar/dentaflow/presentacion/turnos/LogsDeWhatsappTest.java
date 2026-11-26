package ar.dentaflow.presentacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** C-21 spec mensajes-whatsapp "Sin datos personales en logs" (design D7). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LogsDeWhatsappTest {

    private static final String TELEFONO = "1155559999";
    private static final String NOMBRE = "PacienteSensible";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private EscenarioDeAgendaJdbc escenario;
    private long turnoId;
    private String token;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        long paciente = escenario.paciente();
        jdbc.update("UPDATE paciente SET nombre = ?, telefono = ? WHERE id = ?", NOMBRE, TELEFONO, paciente);
        token = escenario.tokenNuevo();
        Instant inicio = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(86_400);
        turnoId = escenario.turno(paciente, escenario.profesional(), escenario.sillon(), escenario.prestacion(30),
                inicio, inicio.plusSeconds(1800), "pendiente", "recepcion", false, escenario.usuario(), token);
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    @Test
    void pedirElMensajeNoDejaTelefonoNombreNiTokenEnElLog(CapturedOutput salida) throws Exception {
        String email = "recepcion-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", Rol.RECEPCION, null).usuario().getId();

        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turnoId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + emisor.emitir(id).token()))
                .andExpect(status().isOk());

        assertThat(salida.getAll())
                .contains("Mensaje de WhatsApp preparado para el turno " + turnoId)
                .doesNotContain(TELEFONO)
                .doesNotContain("549" + TELEFONO)
                .doesNotContain(NOMBRE)
                .doesNotContain(token);
    }
}

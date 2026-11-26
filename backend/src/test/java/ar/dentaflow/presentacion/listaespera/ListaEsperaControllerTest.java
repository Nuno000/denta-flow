package ar.dentaflow.presentacion.listaespera;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ListaEsperaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    @Autowired
    private PacienteRepositorio pacientes;

    @Autowired
    private PrestacionRepositorio prestaciones;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private JdbcTemplate jdbc;

    private String recepcionToken;
    private String profesionalToken;
    private long pacienteId;
    private long prestacionId;
    private long profesionalId;
    private long recepcionUsuarioId;
    private long profesionalUsuarioId;

    @BeforeEach
    void preparar() {
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        recepcionUsuarioId = usuarios.crear("rec-" + sufijo + "@example.com", "contrasena-valida", Rol.RECEPCION, null)
                .usuario().getId();
        recepcionToken = "Bearer " + emisor.emitir(recepcionUsuarioId).token();

        Profesional prof = profesionales.guardar(Profesional.nuevo("Maria", "Lopez", "MP-" + sufijo,
                "maria-" + sufijo + "@example.com"));
        profesionalId = prof.getId();
        profesionalUsuarioId = usuarios.crear("prof-" + sufijo + "@example.com", "contrasena-valida",
                Rol.PROFESIONAL, profesionalId).usuario().getId();
        profesionalToken = "Bearer " + emisor.emitir(profesionalUsuarioId).token();

        String dni = String.valueOf(30000000 + Math.abs(sufijo.hashCode() % 9000000));
        pacienteId = pacientes.guardar(Paciente.nuevo(
                dni, "Ana", "Gomez", "1122334455", "ana-" + sufijo + "@example.com", Instant.now())).getId();
        prestacionId = prestaciones.guardar(Prestacion.nueva("Limpieza " + sufijo, 30)).getId();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM lista_espera WHERE paciente_id = ?", pacienteId);
        jdbc.update("DELETE FROM paciente WHERE id = ?", pacienteId);
        jdbc.update("DELETE FROM prestacion WHERE id = ?", prestacionId);
        jdbc.update("DELETE FROM usuario WHERE id IN (?, ?)", recepcionUsuarioId, profesionalUsuarioId);
        jdbc.update("DELETE FROM profesional WHERE id = ?", profesionalId);
    }

    @Test
    void rechazaSinSesionYConRolProfesional() throws Exception {
        mockMvc.perform(get("/api/lista-espera"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/lista-espera").header("Authorization", profesionalToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void recepcionRegistraListaYDaDeBaja() throws Exception {
        LocalDate hoy = LocalDate.now().plusDays(1);
        LocalDate hasta = hoy.plusDays(5);
        String cuerpo = """
                {
                  "pacienteId": %d,
                  "prestacionId": %d,
                  "fechaDesde": "%s",
                  "fechaHasta": "%s"
                }
                """.formatted(pacienteId, prestacionId, hoy, hasta);

        String respuesta = mockMvc.perform(post("/api/lista-espera")
                        .header("Authorization", recepcionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("activa"))
                .andExpect(jsonPath("$.pacienteNombre").value("Ana Gomez"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = Long.parseLong(respuesta.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mockMvc.perform(get("/api/lista-espera?estado=activa").header("Authorization", recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        mockMvc.perform(delete("/api/lista-espera/" + id).header("Authorization", recepcionToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lista-espera?estado=vencida").header("Authorization", recepcionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }
}

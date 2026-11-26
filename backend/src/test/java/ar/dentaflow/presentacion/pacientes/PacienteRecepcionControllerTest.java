package ar.dentaflow.presentacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec carga-manual-turnos — "Búsqueda de paciente por DNI" ({@code POST /api/pacientes/busqueda}, C-12, design D5).
 * Datos ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Búsqueda de paciente por DNI (HTTP)")
class PacienteRecepcionControllerTest {

    private static final String RUTA = "/api/pacientes/busqueda";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private final Logger raiz = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

    private EscenarioDeAgendaJdbc escenario;
    private String recepcion;
    private String admin;
    private String profesional;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        recepcion = token(Rol.RECEPCION, null);
        admin = token(Rol.ADMINISTRADOR, null);
        profesional = token(Rol.PROFESIONAL, escenario.profesional());
        logs.start();
        raiz.addAppender(logs);
    }

    @AfterEach
    void soltarLogs() {
        raiz.detachAppender(logs);
    }

    private String token(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        return "Bearer " + emisor.emitir(usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario()
                .getId()).token();
    }

    private ResultActions buscar(String dni, String bearer) throws Exception {
        var pedido = post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{\"dni\":\"" + dni + "\"}");
        return mockMvc.perform(bearer == null ? pedido : pedido.header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private static String conPuntos(String dni) {
        return dni.length() == 8 ? dni.substring(0, 2) + "." + dni.substring(2, 5) + "." + dni.substring(5)
                : dni.substring(0, 1) + "." + dni.substring(1, 4) + "." + dni.substring(4);
    }

    @Test
    @DisplayName("un DNI con puntos encuentra al paciente: 200 con DNI normalizado, contacto y Cache-Control no-store")
    void pacienteEncontrado() throws Exception {
        long id = escenario.paciente();
        Map<String, Object> fila = jdbc.queryForMap("SELECT dni, telefono, email FROM paciente WHERE id = ?", id);
        String dni = (String) fila.get("dni");

        buscar(conPuntos(dni), recepcion)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.dni").value(dni))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Ejemplo"))
                .andExpect(jsonPath("$.telefono").value(fila.get("telefono")))
                .andExpect(jsonPath("$.email").value(fila.get("email")));
    }

    @Test
    @DisplayName("el administrador también puede buscar")
    void buscaElAdministrador() throws Exception {
        long id = escenario.paciente();
        String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, id);

        buscar(dni, admin).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("un DNI que no está registrado: 404")
    void pacienteInexistente() throws Exception {
        buscar(escenario.dniNuevo(), recepcion).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No hay un paciente con ese DNI"));
    }

    @Test
    @DisplayName("un paciente anonimizado (sin DNI) no se encuentra")
    void pacienteAnonimizadoNoSeEncuentra() throws Exception {
        long id = escenario.paciente();
        String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, id);
        jdbc.update("UPDATE paciente SET dni = NULL, anonimizado_en = CURRENT_TIMESTAMP, anonimizado_por = ? "
                + "WHERE id = ?", escenario.usuario(), id);

        buscar(dni, recepcion).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un DNI mal formado: 400 sin repetir el valor en la respuesta")
    void dniMalFormado() throws Exception {
        String cuerpo = buscar("30A23456", recepcion).andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("30A23456");
    }

    @Test
    @DisplayName("un DNI vacío o un cuerpo sin DNI: 400")
    void dniVacio() throws Exception {
        buscar("", recepcion).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errores.dni").exists());
        mockMvc.perform(post(RUTA).contentType(MediaType.APPLICATION_JSON).content("{}")
                .header(HttpHeaders.AUTHORIZATION, recepcion)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("el profesional recibe 403 y sin sesión 401")
    void rolesYSesion() throws Exception {
        String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, escenario.paciente());

        buscar(dni, profesional).andExpect(status().isForbidden());
        buscar(dni, null).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("la búsqueda por GET con el DNI en la URL no existe")
    void noHayBusquedaPorUrl() throws Exception {
        String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, escenario.paciente());

        mockMvc.perform(get("/api/pacientes").param("dni", dni).header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("el DNI buscado no se escribe en los logs")
    void sinDniEnLosLogs() throws Exception {
        long id = escenario.paciente();
        String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, id);

        buscar(conPuntos(dni), recepcion).andExpect(status().isOk());
        buscar(escenario.dniNuevo(), recepcion).andExpect(status().isNotFound());
        buscar("30A23456", recepcion).andExpect(status().isBadRequest());

        assertThat(logs.list).extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(mensaje -> mensaje.contains(dni) || mensaje.contains(conPuntos(dni))
                        || mensaje.contains("30A23456"));
    }

    @Test
    @DisplayName("OpenAPI documenta la búsqueda con sus respuestas y el DNI solo en el cuerpo")
    void openApiDocumentaLaBusqueda() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.tags[0]").value("Pacientes"))
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.requestBody").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.responses.200").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.responses.400").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.responses.401").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.responses.403").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.responses.404").exists())
                .andExpect(jsonPath("$.paths['/api/pacientes/busqueda'].post.parameters").doesNotExist());
    }
}

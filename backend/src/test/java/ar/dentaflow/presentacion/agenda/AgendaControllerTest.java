package ar.dentaflow.presentacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Spec agenda-interna — {@code GET /api/agenda/turnos}: contrato JSON, errores y permisos por rol (design D1, D2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AgendaControllerTest {

    private static final String RUTA = "/api/agenda/turnos";
    private static final String DIA = "2031-03-04";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager em;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long laura;
    private long martin;
    private long sillon;
    private long prestacion;
    private long creador;
    private String recepcion;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        laura = escenario.profesional();
        martin = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        creador = escenario.usuario();
        recepcion = token(Rol.RECEPCION, null);
    }

    private String token(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario().getId();
        return "Bearer " + emisor.emitir(id).token();
    }

    private long turno(long profesional, Instant inicio, EstadoTurno estado, boolean sobreturno) {
        Turno turno = Turno.nuevo(paciente, profesional, sillon, prestacion, inicio, 30, OrigenTurno.RECEPCION,
                sobreturno, creador, Instant.parse("2031-01-01T12:00:00Z"), escenario.tokenNuevo());
        turno.cambiarEstado(estado);
        em.persist(turno);
        em.flush();
        return turno.getId();
    }

    private MockHttpServletRequestBuilder agenda(String token) {
        MockHttpServletRequestBuilder pedido = get(RUTA).param("desde", DIA).param("hasta", DIA);
        return token == null ? pedido : pedido.header(HttpHeaders.AUTHORIZATION, token);
    }

    @Test
    void devuelve_el_contrato_de_la_agenda_con_instantes_en_utc() throws Exception {
        long id = turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.CONFIRMADO, true);

        String cuerpo = mockMvc.perform(agenda(recepcion).param("profesionalId", Long.toString(laura)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].inicio").value("2031-03-04T12:00:00Z"))
                .andExpect(jsonPath("$[0].fin").value("2031-03-04T12:30:00Z"))
                .andExpect(jsonPath("$[0].estado").value("confirmado"))
                .andExpect(jsonPath("$[0].origen").value("recepcion"))
                .andExpect(jsonPath("$[0].esSobreturno").value(true))
                .andExpect(jsonPath("$[0].paciente.nombre").value("Ana"))
                .andExpect(jsonPath("$[0].paciente.apellido").value("Ejemplo"))
                .andExpect(jsonPath("$[0].prestacion.id").value(prestacion))
                .andExpect(jsonPath("$[0].profesional.id").value(laura))
                .andExpect(jsonPath("$[0].profesional.nombre").value("Laura"))
                .andExpect(jsonPath("$[0].profesional.apellido").value("Ejemplo"))
                .andExpect(jsonPath("$[0].sillon.id").value(sillon))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo.toLowerCase()).doesNotContain("dni", "telefono", "email", "token", "1155550000");
    }

    @Test
    void sin_estado_oculta_los_cancelados_y_filtrando_los_muestra() throws Exception {
        long confirmado = turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.CONFIRMADO, false);
        long cancelado = turno(laura, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.CANCELADO, false);

        mockMvc.perform(agenda(recepcion).param("profesionalId", Long.toString(laura)))
                .andExpect(jsonPath("$[*].id", contains((int) confirmado)));
        mockMvc.perform(agenda(recepcion).param("profesionalId", Long.toString(laura)).param("estado", "cancelado"))
                .andExpect(jsonPath("$[*].id", contains((int) cancelado)));
    }

    @Test
    void un_paciente_anonimizado_se_muestra_con_sus_valores_sustitutos() throws Exception {
        em.find(Paciente.class, paciente).anonimizar(creador, Instant.parse("2031-01-01T12:00:00Z"));
        em.flush();
        turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.ASISTIO, false);

        String cuerpo = mockMvc.perform(agenda(recepcion).param("profesionalId", Long.toString(laura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paciente.nombre").value("Paciente"))
                .andExpect(jsonPath("$[0].paciente.apellido").value("Anonimizado"))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("dentaflow.invalid", "00000000");
    }

    @Test
    void sin_token_responde_401() throws Exception {
        mockMvc.perform(agenda(null)).andExpect(status().isUnauthorized());
    }

    @Test
    void fecha_mal_formada_responde_400_en_formato_de_problema() throws Exception {
        mockMvc.perform(get(RUTA).param("desde", "2031-02-30").param("hasta", DIA)
                        .header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void falta_una_fecha_responde_400() throws Exception {
        mockMvc.perform(get(RUTA).param("desde", DIA).header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rango_de_mas_de_7_dias_responde_400() throws Exception {
        mockMvc.perform(get(RUTA).param("desde", "2031-03-01").param("hasta", "2031-03-08")
                        .header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El rango de la agenda no puede superar 7 días"));
    }

    @Test
    void estado_desconocido_responde_400_sin_repetir_el_valor() throws Exception {
        String cuerpo = mockMvc.perform(agenda(recepcion).param("estado", "borrado"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("borrado");
    }

    @Test
    void recepcion_ve_los_turnos_de_todos_los_profesionales() throws Exception {
        long deLaura = turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.PENDIENTE, false);
        long deMartin = turno(martin, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.PENDIENTE, false);

        mockMvc.perform(agenda(recepcion).param("sillonId", Long.toString(sillon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains((int) deLaura, (int) deMartin)));
    }

    @Test
    void el_profesional_sin_filtro_ve_solo_sus_turnos() throws Exception {
        long deLaura = turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.PENDIENTE, false);
        turno(martin, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.PENDIENTE, false);

        mockMvc.perform(agenda(token(Rol.PROFESIONAL, laura)).param("sillonId", Long.toString(sillon)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains((int) deLaura)));
    }

    @Test
    void el_profesional_que_pide_la_agenda_de_otro_recibe_403_sin_turnos() throws Exception {
        turno(martin, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.PENDIENTE, false);

        String cuerpo = mockMvc.perform(agenda(token(Rol.PROFESIONAL, laura)).param("profesionalId",
                        Long.toString(martin)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("Ana");
    }

    @Test
    void no_hay_endpoint_de_borrado_en_la_agenda() throws Exception {
        mockMvc.perform(delete(RUTA).header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void la_documentacion_openapi_lista_la_ruta_de_la_agenda() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/agenda/turnos'].get.tags[0]").value("Agenda"));
    }
}

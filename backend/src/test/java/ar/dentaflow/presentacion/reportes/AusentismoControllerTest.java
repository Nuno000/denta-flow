package ar.dentaflow.presentacion.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.transaction.annotation.Transactional;

/** Spec panel-ausentismo — {@code GET /api/reportes/ausentismo}: contrato JSON, RBAC, privacidad y OpenAPI. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AusentismoControllerTest {

    private static final String RUTA = "/api/reportes/ausentismo";
    private static final String DESDE = "2031-03-01";
    private static final String HASTA = "2031-03-31";

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
    private String admin;
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
        admin = token(Rol.ADMINISTRADOR, null);
        recepcion = token(Rol.RECEPCION, null);
    }

    private String token(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario().getId();
        return "Bearer " + emisor.emitir(id).token();
    }

    private void turno(long profesionalId, Instant inicio, EstadoTurno estado) {
        Turno turno = Turno.nuevo(paciente, profesionalId, sillon, prestacion, inicio, 30, OrigenTurno.RECEPCION,
                false, creador, Instant.parse("2031-01-01T12:00:00Z"), escenario.tokenNuevo());
        turno.cambiarEstado(estado);
        em.persist(turno);
        em.flush();
    }

    @Test
    void administrador_obtiene_metricas_agregadas_sin_datos_sensibles() throws Exception {
        // 2 ausentes y 1 asistió con Laura (66.7 %), 1 asistió con Martín (0.0 %) -> general 2/4 = 50.0 %
        turno(laura, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.AUSENTE);
        turno(laura, Instant.parse("2031-03-05T17:00:00Z"), EstadoTurno.AUSENTE_CON_AVISO);
        turno(laura, Instant.parse("2031-03-06T22:00:00Z"), EstadoTurno.ASISTIO);
        turno(martin, Instant.parse("2031-03-06T14:00:00Z"), EstadoTurno.ASISTIO);
        turno(laura, Instant.parse("2031-03-07T14:00:00Z"), EstadoTurno.CANCELADO);

        String cuerpo = mockMvc.perform(get(RUTA)
                        .param("desde", DESDE)
                        .param("hasta", HASTA)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.desde").value(DESDE))
                .andExpect(jsonPath("$.hasta").value(HASTA))
                .andExpect(jsonPath("$.minimoAusencias").value(2))
                .andExpect(jsonPath("$.general.totalEvaluados").value(4))
                .andExpect(jsonPath("$.general.asistidos").value(2))
                .andExpect(jsonPath("$.general.ausentesSinAviso").value(1))
                .andExpect(jsonPath("$.general.ausentesConAviso").value(1))
                .andExpect(jsonPath("$.general.totalAusentes").value(2))
                .andExpect(jsonPath("$.general.tasaAusentismo").value(50.0))
                .andExpect(jsonPath("$.porProfesional", hasSize(2)))
                .andExpect(jsonPath("$.porProfesional[0].profesionalId").value(laura))
                .andExpect(jsonPath("$.porProfesional[0].tasaAusentismo").value(66.7))
                .andExpect(jsonPath("$.porFranja", hasSize(3)))
                .andExpect(jsonPath("$.porFranja[0].codigo").value("manana"))
                .andExpect(jsonPath("$.porDiaSemana", hasSize(3)))
                .andExpect(jsonPath("$.pacientesReiterados", hasSize(1)))
                .andExpect(jsonPath("$.pacientesReiterados[0].pacienteId").value(paciente))
                .andExpect(jsonPath("$.pacientesReiterados[0].nombre").value("Ana"))
                .andExpect(jsonPath("$.pacientesReiterados[0].apellido").value("Ejemplo"))
                .andExpect(jsonPath("$.pacientesReiterados[0].totalAusentes").value(2))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo.toLowerCase()).doesNotContain("dni", "telefono", "email", "token", "1155550000");
    }

    @Test
    void recepcion_puede_consultar_y_filtrar_por_profesional() throws Exception {
        turno(laura, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.AUSENTE);
        turno(martin, Instant.parse("2031-03-04T14:00:00Z"), EstadoTurno.ASISTIO);

        mockMvc.perform(get(RUTA)
                        .param("desde", DESDE)
                        .param("hasta", HASTA)
                        .param("profesionalId", Long.toString(laura))
                        .header(HttpHeaders.AUTHORIZATION, recepcion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionalId").value(laura))
                .andExpect(jsonPath("$.general.totalEvaluados").value(1))
                .andExpect(jsonPath("$.general.tasaAusentismo").value(100.0));
    }

    @Test
    void profesional_ve_solo_sus_propios_turnos() throws Exception {
        turno(laura, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.ASISTIO);
        turno(martin, Instant.parse("2031-03-04T14:00:00Z"), EstadoTurno.AUSENTE);

        mockMvc.perform(get(RUTA)
                        .param("desde", DESDE)
                        .param("hasta", HASTA)
                        .header(HttpHeaders.AUTHORIZATION, token(Rol.PROFESIONAL, laura)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profesionalId").value(laura))
                .andExpect(jsonPath("$.general.totalEvaluados").value(1))
                .andExpect(jsonPath("$.general.tasaAusentismo").value(0.0));
    }

    @Test
    void profesional_que_pide_otro_profesional_recibe_403_sin_datos() throws Exception {
        turno(martin, Instant.parse("2031-03-04T14:00:00Z"), EstadoTurno.AUSENTE);

        String cuerpo = mockMvc.perform(get(RUTA)
                        .param("desde", DESDE)
                        .param("hasta", HASTA)
                        .param("profesionalId", Long.toString(martin))
                        .header(HttpHeaders.AUTHORIZATION, token(Rol.PROFESIONAL, laura)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("Ana");
    }

    @Test
    void sin_token_responde_401() throws Exception {
        mockMvc.perform(get(RUTA).param("desde", DESDE).param("hasta", HASTA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rango_invertido_o_umbral_invalido_responde_400_en_formato_de_problema() throws Exception {
        mockMvc.perform(get(RUTA)
                        .param("desde", "2031-03-10")
                        .param("hasta", "2031-03-01")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get(RUTA)
                        .param("desde", DESDE)
                        .param("hasta", HASTA)
                        .param("minimoAusencias", "0")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void paciente_anonimizado_aparece_con_valores_sustitutos_sin_filtrar_invalid() throws Exception {
        em.find(Paciente.class, paciente).anonimizar(creador, Instant.parse("2031-01-01T12:00:00Z"));
        em.flush();
        turno(laura, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.AUSENTE);
        turno(laura, Instant.parse("2031-03-05T13:00:00Z"), EstadoTurno.AUSENTE);

        String cuerpo = mockMvc.perform(get(RUTA)
                        .param("desde", DESDE)
                        .param("hasta", HASTA)
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pacientesReiterados[0].nombre").value("Paciente"))
                .andExpect(jsonPath("$.pacientesReiterados[0].apellido").value("Anonimizado"))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpo).doesNotContain("dentaflow.invalid", "00000000");
    }

    @Test
    void openapi_documenta_el_endpoint_de_ausentismo() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/reportes/ausentismo'].get.tags[0]").value("Reportes"));
    }
}

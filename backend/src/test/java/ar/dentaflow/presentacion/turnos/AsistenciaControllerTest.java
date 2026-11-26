package ar.dentaflow.presentacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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

/** Spec asistencia-turnos — {@code POST /api/turnos/{id}/asistencia}: contrato, auditoría, RBAC y errores (D1, D3). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AsistenciaControllerTest {

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
    private UsuarioDePrueba recepcion;
    private UsuarioDePrueba admin;

    private record UsuarioDePrueba(long id, String bearer) {
    }

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        laura = escenario.profesional();
        martin = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        creador = escenario.usuario();
        recepcion = usuario(Rol.RECEPCION, null);
        admin = usuario(Rol.ADMINISTRADOR, null);
    }

    private UsuarioDePrueba usuario(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario().getId();
        return new UsuarioDePrueba(id, "Bearer " + emisor.emitir(id).token());
    }

    /** Crea el turno vía JPA para respetar el mapeo de {@link Instant} (gotcha de zona horaria, C-11 D7). */
    private long turno(long profesional, Instant inicio, EstadoTurno estado) {
        Turno turno = Turno.nuevo(paciente, profesional, sillon, prestacion, inicio, 30, OrigenTurno.RECEPCION,
                false, creador, Instant.now().minusSeconds(7200), escenario.tokenNuevo());
        turno.cambiarEstado(estado);
        em.persist(turno);
        em.flush();
        em.clear();
        return turno.getId();
    }

    private static Instant yaEmpezo() {
        return Instant.now().truncatedTo(ChronoUnit.MINUTES).minusSeconds(600);
    }

    private static Instant futuro() {
        return Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(3600);
    }

    private MockHttpServletRequestBuilder registrar(long turnoId, String cuerpoJson, String bearer) {
        MockHttpServletRequestBuilder pedido = post("/api/turnos/{id}/asistencia", turnoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoJson);
        return bearer == null ? pedido : pedido.header(HttpHeaders.AUTHORIZATION, bearer);
    }

    private String estadoEnBase(long turnoId) {
        em.flush();
        return jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turnoId);
    }

    private List<Map<String, Object>> historial(long turnoId) {
        em.flush();
        return jdbc.queryForList(
                "SELECT actor_tipo, usuario_id, accion, valor_anterior, valor_nuevo "
                        + "FROM turno_historial WHERE turno_id = ? ORDER BY id",
                turnoId);
    }

    @Test
    void recepcion_registra_asistio_con_auditoria_y_sin_datos_personales() throws Exception {
        long id = turno(laura, yaEmpezo(), EstadoTurno.CONFIRMADO);

        String cuerpo = mockMvc.perform(registrar(id, "{\"estado\":\"asistio\"}", recepcion.bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("asistio"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo.toLowerCase()).doesNotContain("dni", "telefono", "email", "token", "ana", "1155550000");
        assertThat(estadoEnBase(id)).isEqualTo("asistio");

        List<Map<String, Object>> filas = historial(id);
        assertThat(filas).hasSize(1);
        assertThat(filas.getFirst().get("actor_tipo")).isEqualTo("usuario");
        assertThat(((Number) filas.getFirst().get("usuario_id")).longValue()).isEqualTo(recepcion.id());
        assertThat(filas.getFirst().get("accion")).isEqualTo("cambio_estado");
        assertThat(filas.getFirst().get("valor_anterior").toString()).contains("confirmado");
        assertThat(filas.getFirst().get("valor_nuevo").toString()).contains("asistio");
    }

    @Test
    void administrador_y_profesional_en_su_turno_pueden_registrar_asistencia() throws Exception {
        long deLaura = turno(laura, yaEmpezo(), EstadoTurno.CONFIRMADO);
        long deMartin = turno(martin, yaEmpezo(), EstadoTurno.PENDIENTE);
        UsuarioDePrueba usuariaLaura = usuario(Rol.PROFESIONAL, laura);

        mockMvc.perform(registrar(deLaura, "{\"estado\":\"ausente\"}", usuariaLaura.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ausente"));

        mockMvc.perform(registrar(deMartin, "{\"estado\":\"asistio\"}", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("asistio"));

        assertThat(((Number) historial(deLaura).getFirst().get("usuario_id")).longValue()).isEqualTo(usuariaLaura.id());
        assertThat(((Number) historial(deMartin).getFirst().get("usuario_id")).longValue()).isEqualTo(admin.id());
    }

    @Test
    void ausente_con_aviso_se_acepta_antes_del_inicio() throws Exception {
        long id = turno(laura, futuro(), EstadoTurno.CONFIRMADO);

        mockMvc.perform(registrar(id, "{\"estado\":\"ausente_con_aviso\"}", recepcion.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("ausente_con_aviso"));

        assertThat(estadoEnBase(id)).isEqualTo("ausente_con_aviso");
        assertThat(historial(id)).hasSize(1);
    }

    @Test
    void corregir_asistencia_audita_el_cambio_y_repetir_el_estado_es_idempotente() throws Exception {
        long id = turno(laura, yaEmpezo(), EstadoTurno.ASISTIO);

        // Repetir el mismo estado: 200 sin fila nueva en historial
        mockMvc.perform(registrar(id, "{\"estado\":\"asistio\"}", recepcion.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("asistio"));
        assertThat(historial(id)).isEmpty();

        // Corregir asistio -> ausente: 200 con fila nueva de auditoría
        mockMvc.perform(registrar(id, "{\"estado\":\"ausente\"}", recepcion.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.estado").value("ausente"));

        assertThat(estadoEnBase(id)).isEqualTo("ausente");
        List<Map<String, Object>> filas = historial(id);
        assertThat(filas).hasSize(1);
        assertThat(filas.getFirst().get("valor_anterior").toString()).contains("asistio");
        assertThat(filas.getFirst().get("valor_nuevo").toString()).contains("ausente");
    }

    @Test
    void turno_cancelado_responde_409_sin_cambios_ni_historial() throws Exception {
        long id = turno(laura, yaEmpezo(), EstadoTurno.CANCELADO);

        mockMvc.perform(registrar(id, "{\"estado\":\"asistio\"}", recepcion.bearer()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(estadoEnBase(id)).isEqualTo("cancelado");
        assertThat(historial(id)).isEmpty();
    }

    @Test
    void asistio_en_turno_futuro_responde_409_sin_cambios_ni_historial() throws Exception {
        long id = turno(laura, futuro(), EstadoTurno.CONFIRMADO);

        mockMvc.perform(registrar(id, "{\"estado\":\"asistio\"}", recepcion.bearer()))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("La asistencia se registra desde el inicio del turno"));

        assertThat(estadoEnBase(id)).isEqualTo("confirmado");
        assertThat(historial(id)).isEmpty();
    }

    @Test
    void profesional_en_turno_ajeno_responde_403_sin_cambios_ni_historial() throws Exception {
        long deMartin = turno(martin, yaEmpezo(), EstadoTurno.CONFIRMADO);
        UsuarioDePrueba usuariaLaura = usuario(Rol.PROFESIONAL, laura);

        mockMvc.perform(registrar(deMartin, "{\"estado\":\"asistio\"}", usuariaLaura.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        assertThat(estadoEnBase(deMartin)).isEqualTo("confirmado");
        assertThat(historial(deMartin)).isEmpty();
    }

    @Test
    void sin_token_responde_401_sin_cambios_ni_historial() throws Exception {
        long id = turno(laura, yaEmpezo(), EstadoTurno.CONFIRMADO);

        mockMvc.perform(registrar(id, "{\"estado\":\"asistio\"}", null))
                .andExpect(status().isUnauthorized());

        assertThat(estadoEnBase(id)).isEqualTo("confirmado");
        assertThat(historial(id)).isEmpty();
    }

    @Test
    void estado_invalido_o_cuerpo_vacio_responde_400_sin_repetir_el_valor_ni_auditar() throws Exception {
        long id = turno(laura, yaEmpezo(), EstadoTurno.CONFIRMADO);

        String cuerpoInvalido = mockMvc.perform(registrar(id, "{\"estado\":\"borrado\"}", recepcion.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpoInvalido).doesNotContain("borrado");

        mockMvc.perform(registrar(id, "{\"estado\":\"confirmado\"}", recepcion.bearer()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(registrar(id, "{\"estado\":\"\"}", recepcion.bearer()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(registrar(id, "{}", recepcion.bearer()))
                .andExpect(status().isBadRequest());

        assertThat(estadoEnBase(id)).isEqualTo("confirmado");
        assertThat(historial(id)).isEmpty();
    }

    @Test
    void turno_inexistente_responde_404() throws Exception {
        mockMvc.perform(registrar(999_999L, "{\"estado\":\"asistio\"}", recepcion.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void la_documentacion_openapi_lista_la_ruta_de_asistencia() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.tags[0]").value("Turnos"))
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.responses['404']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/asistencia'].post.responses['409']").exists());
    }
}

package ar.dentaflow.presentacion.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import ar.dentaflow.infraestructura.seguridad.EmisorDeTokensJwt;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

/** Spec auditoria-turnos (US-025) — {@code GET /api/turnos/{id}/historial} solo para administrador (design D2). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HistorialTurnoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ServicioDeTurnos turnos;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private EmisorDeTokensJwt emisor;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profesional;
    private long sillon;
    private long prestacion;
    private UsuarioDePrueba admin;
    private UsuarioDePrueba recepcion;
    private UsuarioDePrueba profesionalUsuario;

    private record UsuarioDePrueba(long id, String email, String bearer) {
    }

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(profesional, prestacion);
        escenario.atiendeTodaLaSemana(profesional);
        admin = crearUsuario(Rol.ADMINISTRADOR, null);
        recepcion = crearUsuario(Rol.RECEPCION, null);
        profesionalUsuario = crearUsuario(Rol.PROFESIONAL, profesional);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM turno_historial WHERE turno_id IN (SELECT id FROM turno WHERE profesional_id = ?)",
                profesional);
        jdbc.update("DELETE FROM turno WHERE profesional_id = ?", profesional);
        jdbc.update("DELETE FROM usuario WHERE id IN (?, ?, ?)",
                admin.id(), recepcion.id(), profesionalUsuario.id());
        escenario.limpiar();
    }

    private UsuarioDePrueba crearUsuario(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario().getId();
        return new UsuarioDePrueba(id, email, "Bearer " + emisor.emitir(id).token());
    }

    private long crearTurnoConHistorial() {
        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(5);
        Instant inicio = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(fecha, LocalTime.of(10, 0)));
        var creado = turnos.crear(new SolicitudDeTurno(
                paciente, profesional, sillon, prestacion, inicio, OrigenTurno.ONLINE, false, Actor.paciente()));
        long turnoId = creado.turno().getId();
        turnos.cambiarEstado(turnoId, EstadoTurno.CONFIRMADO, Actor.usuario(recepcion.id()));
        turnos.cambiarEstado(turnoId, EstadoTurno.ASISTIO, Actor.usuario(admin.id()));
        return turnoId;
    }

    @Test
    void administrador_ve_el_historial_en_orden_cronologico_sin_pii_del_paciente_ni_token() throws Exception {
        long turnoId = crearTurnoConHistorial();

        String cuerpo = mockMvc.perform(get("/api/turnos/{id}/historial", turnoId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].accion").value("creacion"))
                .andExpect(jsonPath("$[0].actorTipo").value("paciente"))
                .andExpect(jsonPath("$[0].actorEmail").value(nullValue()))
                .andExpect(jsonPath("$[1].accion").value("confirmacion"))
                .andExpect(jsonPath("$[1].actorTipo").value("usuario"))
                .andExpect(jsonPath("$[1].actorEmail").value(recepcion.email()))
                .andExpect(jsonPath("$[1].valorAnterior.estado").value("pendiente"))
                .andExpect(jsonPath("$[1].valorNuevo.estado").value("confirmado"))
                .andExpect(jsonPath("$[2].accion").value("cambio_estado"))
                .andExpect(jsonPath("$[2].actorTipo").value("usuario"))
                .andExpect(jsonPath("$[2].actorEmail").value(admin.email()))
                .andExpect(jsonPath("$[2].valorAnterior.estado").value("confirmado"))
                .andExpect(jsonPath("$[2].valorNuevo.estado").value("asistio"))
                .andReturn().getResponse().getContentAsString();

        assertThat(cuerpo.toLowerCase()).doesNotContain("dni", "telefono", "1155550000", "token", "pac-");
    }

    @Test
    void recepcion_y_profesional_reciben_403_sin_registros() throws Exception {
        long turnoId = crearTurnoConHistorial();

        String cuerpoRecepcion = mockMvc.perform(get("/api/turnos/{id}/historial", turnoId)
                        .header(HttpHeaders.AUTHORIZATION, recepcion.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();
        assertThat(cuerpoRecepcion).doesNotContain("creacion");

        mockMvc.perform(get("/api/turnos/{id}/historial", turnoId)
                        .header(HttpHeaders.AUTHORIZATION, profesionalUsuario.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void sin_token_responde_401_y_turno_inexistente_responde_404() throws Exception {
        long turnoId = crearTurnoConHistorial();

        mockMvc.perform(get("/api/turnos/{id}/historial", turnoId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/turnos/{id}/historial", 999_999L)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void no_hay_operaciones_de_edicion_ni_borrado_sobre_el_historial() throws Exception {
        long turnoId = crearTurnoConHistorial();

        mockMvc.perform(delete("/api/turnos/{id}/historial", turnoId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer()))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(put("/api/turnos/{id}/historial", turnoId)
                        .header(HttpHeaders.AUTHORIZATION, admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void la_documentacion_openapi_expone_el_endpoint_de_historial() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/historial'].get.tags[0]").value("Auditoría"))
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/historial'].get.responses['200']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/historial'].get.responses['401']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/historial'].get.responses['403']").exists())
                .andExpect(jsonPath("$.paths['/api/turnos/{id}/historial'].get.responses['404']").exists());
    }
}

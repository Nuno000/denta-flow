package ar.dentaflow.presentacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** C-21 spec mensajes-whatsapp — {@code GET /api/turnos/{id}/whatsapp}: contrato, RBAC y errores (design D1). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WhatsappControllerTest {

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
    private long profesional;
    private long sillon;
    private long prestacion;
    private long creador;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        creador = escenario.usuario();
    }

    private String bearer(Rol rol, Long profesionalId) {
        String email = rol.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        long id = usuarios.crear(email, "contrasena-valida", rol, profesionalId).usuario().getId();
        return "Bearer " + emisor.emitir(id).token();
    }

    private long turno(EstadoTurno estado) {
        Instant inicio = Instant.now().truncatedTo(ChronoUnit.MINUTES).plusSeconds(86_400);
        Turno turno = Turno.nuevo(paciente, profesional, sillon, prestacion, inicio, 30, OrigenTurno.RECEPCION,
                false, creador, Instant.now().minusSeconds(7200), escenario.tokenNuevo());
        turno.cambiarEstado(estado);
        em.persist(turno);
        em.flush();
        em.clear();
        return turno.getId();
    }

    private long cambiosDe(long turnoId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id = ?", Long.class, turnoId);
    }

    @Test
    void recepcionRecibeElMensajeYElEnlaceSinCacheYSinTocarElHistorial() throws Exception {
        long turnoId = turno(EstadoTurno.PENDIENTE);
        long cambiosAntes = cambiosDe(turnoId);

        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turnoId).header(HttpHeaders.AUTHORIZATION,
                        bearer(Rol.RECEPCION, null)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.mensaje", containsString("Hola Ana")))
                .andExpect(jsonPath("$.mensaje", containsString("/turno/")))
                .andExpect(jsonPath("$.enlace", startsWith("https://wa.me/5491155550000?text=Hola%20Ana")));

        assertThat(cambiosDe(turnoId)).isEqualTo(cambiosAntes);
    }

    @Test
    void elAdministradorTambienPuede() throws Exception {
        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turno(EstadoTurno.CONFIRMADO))
                        .header(HttpHeaders.AUTHORIZATION, bearer(Rol.ADMINISTRADOR, null)))
                .andExpect(status().isOk());
    }

    @Test
    void sinTelefonoValidoElEnlaceEsNulo() throws Exception {
        jdbc.update("UPDATE paciente SET telefono = '+59899123456' WHERE id = ?", paciente);

        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turno(EstadoTurno.PENDIENTE))
                        .header(HttpHeaders.AUTHORIZATION, bearer(Rol.RECEPCION, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enlace").value(nullValue()))
                .andExpect(jsonPath("$.mensaje", containsString("Hola Ana")));
    }

    @Test
    void elProfesionalNoPuedeNiSiquieraConUnTurnoPropio() throws Exception {
        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turno(EstadoTurno.PENDIENTE))
                        .header(HttpHeaders.AUTHORIZATION, bearer(Rol.PROFESIONAL, profesional)))
                .andExpect(status().isForbidden());
    }

    @Test
    void sinSesionDa401() throws Exception {
        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turno(EstadoTurno.PENDIENTE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTurnoInexistenteDa404() throws Exception {
        mockMvc.perform(get("/api/turnos/{id}/whatsapp", 987_654_321L)
                        .header(HttpHeaders.AUTHORIZATION, bearer(Rol.RECEPCION, null)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unTurnoCanceladoDa409() throws Exception {
        mockMvc.perform(get("/api/turnos/{id}/whatsapp", turno(EstadoTurno.CANCELADO))
                        .header(HttpHeaders.AUTHORIZATION, bearer(Rol.RECEPCION, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("pendientes o confirmados")));
    }
}

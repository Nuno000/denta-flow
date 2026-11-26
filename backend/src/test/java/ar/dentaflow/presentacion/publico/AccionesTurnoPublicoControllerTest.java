package ar.dentaflow.presentacion.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class AccionesTurnoPublicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long prof;
    private long sillon;
    private long prestacion;
    private long paciente;
    private long turnoId;
    private String token;
    private Instant inicioTurno;
    private Instant finTurno;

    @BeforeEach
    void setUp() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        prof = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(prof, prestacion);
        escenario.atiendeTodaLaSemana(prof);

        paciente = escenario.paciente();
        token = escenario.tokenNuevo();

        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(10);
        inicioTurno = LocalDateTime.of(fecha, LocalTime.of(10, 0))
                .atZone(ZonaHorariaConsultorio.ZONA).toInstant();
        finTurno = inicioTurno.plusSeconds(1800);

        turnoId = escenario.turno(paciente, prof, sillon, prestacion, inicioTurno, finTurno,
                "pendiente", "recepcion", false, escenario.usuario(), token);
    }

    @AfterEach
    void tearDown() {
        escenario.limpiar();
    }

    @Test
    void consultarTurnoValidoDevuelveDatosSinExponerDatosSensibles() throws Exception {
        mockMvc.perform(get("/api/publico/turnos/" + token)
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.1");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacion").value("VIGENTE"))
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.prestacion").exists())
                .andExpect(jsonPath("$.profesional").exists())
                .andExpect(jsonPath("$.puedeModificar").value(true))
                .andExpect(jsonPath("$.dni").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.telefono").doesNotExist())
                .andExpect(jsonPath("$.sillon").doesNotExist())
                .andExpect(jsonPath("$.sillonId").doesNotExist());
    }

    @Test
    void consultarTokenInvalidoDevuelve404() throws Exception {
        mockMvc.perform(get("/api/publico/turnos/token-inexistente-xyz")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.2");
                            return req;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void confirmarTurnoPasaAConfirmadoYSegundaLlamadaEsIdempotente() throws Exception {
        mockMvc.perform(post("/api/publico/turnos/" + token + "/confirmar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.3");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("confirmado"));

        String estadoEnBase = jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turnoId);
        assertThat(estadoEnBase).isEqualTo("confirmado");

        // Idempotente
        mockMvc.perform(post("/api/publico/turnos/" + token + "/confirmar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.3");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("confirmado"));

        Long auditoriasConfirmacion = jdbc.queryForObject(
                "SELECT COUNT(*) FROM turno_historial WHERE turno_id = ? AND accion = 'confirmacion'",
                Long.class, turnoId);
        assertThat(auditoriasConfirmacion).isEqualTo(1L);
    }

    @Test
    void cancelarTurnoValidoPasaACanceladoYLiberaRecursos() throws Exception {
        mockMvc.perform(post("/api/publico/turnos/" + token + "/cancelar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.4");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("cancelado"))
                .andExpect(jsonPath("$.puedeModificar").value(false));

        String estadoEnBase = jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, turnoId);
        assertThat(estadoEnBase).isEqualTo("cancelado");

        // Cancelar de nuevo devuelve 409
        mockMvc.perform(post("/api/publico/turnos/" + token + "/cancelar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.4");
                            return req;
                        }))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelarTurnoFueraDeTerminoDevuelve409ConMensajeYContacto() throws Exception {
        // Configuramos turno para dentro de 2 horas (anticipación mínima default es 24h)
        Instant inicioCercano = Instant.now().plusSeconds(7200);
        String tokenCercano = escenario.tokenNuevo();
        escenario.turno(paciente, prof, sillon, prestacion, inicioCercano, inicioCercano.plusSeconds(1800),
                "pendiente", "recepcion", false, escenario.usuario(), tokenCercano);

        mockMvc.perform(post("/api/publico/turnos/" + tokenCercano + "/cancelar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.5");
                            return req;
                        }))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail", containsString("vencido")))
                .andExpect(jsonPath("$.detail", containsString("consultorio")));
    }

    @Test
    void listarHuecosExcluyeElPropioTurnoYDevuelve200() throws Exception {
        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(10);
        String fechaStr = fecha.format(DateTimeFormatter.ISO_LOCAL_DATE);

        mockMvc.perform(get("/api/publico/turnos/" + token + "/huecos")
                        .param("desde", fechaStr)
                        .param("hasta", fechaStr)
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.6");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void reprogramarTurnoValidoActualizaHorarioYAudita() throws Exception {
        LocalDate fechaNueva = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(11);
        LocalDateTime ldtNuevo = LocalDateTime.of(fechaNueva, LocalTime.of(14, 0));
        String nuevoInicioIso = ldtNuevo.atZone(ZonaHorariaConsultorio.ZONA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        mockMvc.perform(post("/api/publico/turnos/" + token + "/reprogramar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"" + nuevoInicioIso + "\"}")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.7");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio", containsString("14:00")));

        Long auditoriasReprogramacion = jdbc.queryForObject(
                "SELECT COUNT(*) FROM turno_historial WHERE turno_id = ? AND accion = 'reprogramacion'",
                Long.class, turnoId);
        assertThat(auditoriasReprogramacion).isEqualTo(1L);
    }

    @Test
    void reprogramarAHorarioTomadoDevuelve409() throws Exception {
        // Creamos otro turno en el horario objetivo
        LocalDate fechaNueva = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(12);
        Instant inicioOcupado = LocalDateTime.of(fechaNueva, LocalTime.of(15, 0))
                .atZone(ZonaHorariaConsultorio.ZONA).toInstant();
        escenario.turno(escenario.paciente(), prof, sillon, prestacion, inicioOcupado, inicioOcupado.plusSeconds(1800),
                "pendiente", "recepcion", false, escenario.usuario(), escenario.tokenNuevo());

        String ocupadoIso = LocalDateTime.of(fechaNueva, LocalTime.of(15, 0))
                .atZone(ZonaHorariaConsultorio.ZONA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        mockMvc.perform(post("/api/publico/turnos/" + token + "/reprogramar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\":\"" + ocupadoIso + "\"}")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.8");
                            return req;
                        }))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("no está disponible")));
    }

    @Test
    void rateLimiterPorIpResponde429AlExcederLimiteEnAcciones() throws Exception {
        String ipAbusiva = "198.51.100.99";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/publico/turnos/" + token + "/confirmar")
                            .with(req -> {
                                req.setRemoteAddr(ipAbusiva);
                                return req;
                            }))
                    .andExpect(status().isOk());
        }

        // 6to pedido de acción recibe 429
        mockMvc.perform(post("/api/publico/turnos/" + token + "/confirmar")
                        .with(req -> {
                            req.setRemoteAddr(ipAbusiva);
                            return req;
                        }))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title").value("Demasiados pedidos"));
    }

    @Test
    void consultasDeLecturaNoConsumenElLimiteDeAcciones() throws Exception {
        String ip = "198.51.100.77";
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(get("/api/publico/turnos/" + token)
                            .with(req -> {
                                req.setRemoteAddr(ip);
                                return req;
                            }))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/publico/turnos/" + token + "/confirmar")
                        .with(req -> {
                            req.setRemoteAddr(ip);
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void cancelarTurnoYaCanceladoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/publico/turnos/" + token + "/cancelar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.21");
                            return req;
                        }))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/publico/turnos/" + token + "/cancelar")
                        .with(req -> {
                            req.setRemoteAddr("192.0.2.21");
                            return req;
                        }))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail", containsString("cancelado")));
    }
}

package ar.dentaflow.presentacion.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Tarea 5.4: Concurrencia entre nueva reserva online y reprogramación de turno paciente
 * compitiendo por el mismo hueco en MySQL 8 real (@Tag("mysql"), profile "it-mysql").
 * Exactamente uno obtiene 200/201 y el otro 409, sin solapamientos en base de datos.
 */
@Tag("mysql")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it-mysql")
class ReprogramacionConcurrenciaMySqlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long prof;
    private long sillon;
    private long prestacion;
    private String fechaHoraIso;
    private String tokenTurnoAReprogramar;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        prof = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(prof, prestacion);
        escenario.atiendeTodaLaSemana(prof);

        // Horario disputado: día +36 a las 10:00
        LocalDate fechaDisputada = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(36);
        LocalDateTime ldtDisputada = LocalDateTime.of(fechaDisputada, LocalTime.of(10, 0));
        fechaHoraIso = ldtDisputada.atZone(ZonaHorariaConsultorio.ZONA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // Turno existente a reprogramar: día +36 a las 16:00
        tokenTurnoAReprogramar = escenario.tokenNuevo();
        Instant inicioOriginal = LocalDateTime.of(fechaDisputada, LocalTime.of(16, 0))
                .atZone(ZonaHorariaConsultorio.ZONA).toInstant();
        Instant finOriginal = inicioOriginal.plusSeconds(1800);
        escenario.turno(escenario.paciente(), prof, sillon, prestacion, inicioOriginal, finOriginal,
                "pendiente", "recepcion", false, escenario.usuario(), tokenTurnoAReprogramar);
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    private String jsonReserva(String dni, String email) {
        return """
                {
                  "prestacionId": %d,
                  "profesionalId": %d,
                  "inicio": "%s",
                  "paciente": {
                    "dni": "%s",
                    "nombre": "Concurrente",
                    "apellido": "Test",
                    "telefono": "0261 555-0000",
                    "email": "%s"
                  },
                  "aceptaAvisoPrivacidad": true
                }
                """.formatted(prestacion, prof, fechaHoraIso, dni, email);
    }

    private record RespuestaHttp(int status, String cuerpo) {}

    @RepeatedTest(5)
    void reservaYReprogramacionSimultaneasAlMismoHuecoTerminanEnUnExitoUn409YUnSoloTurno() throws Exception {
        String dni = escenario.dniNuevo();
        String jsonReserva = jsonReserva(dni, "pacienteConcurrente@example.com");
        String jsonReprogramar = "{\"inicio\":\"" + fechaHoraIso + "\"}";

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // Petición A: Reserva nueva (201 si gana, 409 si pierde)
        Callable<RespuestaHttp> peticionReserva = () -> {
            largada.await();
            MvcResult res = mockMvc.perform(post("/api/publico/reservas")
                            .with(req -> {
                                req.setRemoteAddr("192.0.2.71");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonReserva))
                    .andReturn();
            return new RespuestaHttp(res.getResponse().getStatus(), res.getResponse().getContentAsString());
        };

        // Petición B: Reprogramación (200 si gana, 409 si pierde)
        Callable<RespuestaHttp> peticionReprogramar = () -> {
            largada.await();
            MvcResult res = mockMvc.perform(post("/api/publico/turnos/" + tokenTurnoAReprogramar + "/reprogramar")
                            .with(req -> {
                                req.setRemoteAddr("192.0.2.72");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonReprogramar))
                    .andReturn();
            return new RespuestaHttp(res.getResponse().getStatus(), res.getResponse().getContentAsString());
        };

        try {
            Future<RespuestaHttp> futA = pool.submit(peticionReserva);
            Future<RespuestaHttp> futB = pool.submit(peticionReprogramar);
            largada.countDown();

            RespuestaHttp respReserva = futA.get();
            RespuestaHttp respReprogramar = futB.get();

            int statusReserva = respReserva.status();
            int statusReprogramar = respReprogramar.status();

            boolean reservaExitosa = statusReserva == 201;
            boolean reprogramacionExitosa = statusReprogramar == 200;

            // Exactamente una tiene éxito y la otra es rechazada con 409
            assertThat(reservaExitosa ^ reprogramacionExitosa).isTrue();
            if (reservaExitosa) {
                assertThat(statusReprogramar).isEqualTo(409);
            } else {
                assertThat(statusReserva).isEqualTo(409);
            }

            // En la base queda un solo turno no cancelado en el horario disputado. Hibernate guarda `inicio` en UTC
            // (hibernate.jdbc.time_zone=UTC), así que se compara con la hora UTC y no con un Timestamp en la zona de la JVM.
            LocalDate fechaDisputada = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(36);
            Instant inicioDisputado = LocalDateTime.of(fechaDisputada, LocalTime.of(10, 0))
                    .atZone(ZonaHorariaConsultorio.ZONA).toInstant();

            Long turnosEnHorarioDisputado = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM turno WHERE profesional_id = ? AND inicio = ? AND estado != 'cancelado'",
                    Long.class, prof, LocalDateTime.ofInstant(inicioDisputado, ZoneOffset.UTC));
            assertThat(turnosEnHorarioDisputado).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }
}

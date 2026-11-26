package ar.dentaflow.presentacion.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
 * Tarea 5.7 (spec "Reserva online de huecos", design D8):
 * Concurrencia de reservas públicas simultáneas al mismo hueco contra MySQL 8 real (it-mysql).
 * Dos POST simultáneos con 1 profesional y 1 sillón -> un 201, un 409 y un solo turno activo.
 */
@Tag("mysql")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it-mysql")
class ReservaPublicaConcurrenciaMySqlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long prof;
    private long sillon;
    private long prestacion;
    private String fechaHoraIso;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        prof = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(prof, prestacion);
        escenario.atiendeTodaLaSemana(prof);

        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(35);
        LocalDateTime ldt = LocalDateTime.of(fecha, LocalTime.of(10, 0));
        fechaHoraIso = ldt.atZone(ZonaHorariaConsultorio.ZONA).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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
    void dosReservasSimultaneasAlMismoHuecoTerminanEnUn201Un409YUnSoloTurno() throws Exception {
        String dniA = escenario.dniNuevo();
        String dniB = escenario.dniNuevo();
        String jsonA = jsonReserva(dniA, "pacienteA@example.com");
        String jsonB = jsonReserva(dniB, "pacienteB@example.com");

        CountDownLatch largada = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);

        Callable<RespuestaHttp> peticionA = () -> {
            largada.await();
            MvcResult res = mockMvc.perform(post("/api/publico/reservas")
                            .with(req -> {
                                req.setRemoteAddr("192.0.2.51");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonA))
                    .andReturn();
            return new RespuestaHttp(res.getResponse().getStatus(), res.getResponse().getContentAsString());
        };

        Callable<RespuestaHttp> peticionB = () -> {
            largada.await();
            MvcResult res = mockMvc.perform(post("/api/publico/reservas")
                            .with(req -> {
                                req.setRemoteAddr("192.0.2.52");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonB))
                    .andReturn();
            return new RespuestaHttp(res.getResponse().getStatus(), res.getResponse().getContentAsString());
        };

        try {
            Future<RespuestaHttp> futA = pool.submit(peticionA);
            Future<RespuestaHttp> futB = pool.submit(peticionB);
            largada.countDown();

            RespuestaHttp respA = futA.get();
            RespuestaHttp respB = futB.get();

            List<Integer> statuses = List.of(respA.status(), respB.status());
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);

            Long turnosCreados = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM turno WHERE profesional_id = ? AND estado != 'cancelado'",
                    Long.class, prof);
            assertThat(turnosCreados).isEqualTo(1L);
        } finally {
            pool.shutdownNow();
        }
    }
}

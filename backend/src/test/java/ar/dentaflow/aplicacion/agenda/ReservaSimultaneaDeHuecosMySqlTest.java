package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec motor-disponibilidad — "Reservas simultáneas del mismo hueco" contra MySQL 8 real
 * (perfil {@code it-mysql}, tarea 7.1).
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class ReservaSimultaneaDeHuecosMySqlTest {

    @Autowired
    private ServicioDeReservaDeHuecos reserva;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private Instant inicio;
    private long paciente;
    private long prestacion;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(30);
        inicio = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(fecha, LocalTime.of(10, 0)));
        paciente = escenario.paciente();
        prestacion = escenario.prestacion(30);
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    private long profesional() {
        long id = escenario.profesional();
        escenario.habilitar(id, prestacion);
        escenario.atiendeTodaLaSemana(id);
        return id;
    }

    private record Resultado(TurnoCreado creado, Throwable error) {
        boolean fueExitosa() {
            return error == null;
        }
    }

    private List<Resultado> competir(Callable<TurnoCreado> primera, Callable<TurnoCreado> segunda) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<TurnoCreado> a = hilos.submit(() -> {
                salida.await();
                return primera.call();
            });
            Future<TurnoCreado> b = hilos.submit(() -> {
                salida.await();
                return segunda.call();
            });
            salida.countDown();
            return List.of(resultadoDe(a), resultadoDe(b));
        } finally {
            hilos.shutdownNow();
        }
    }

    private static Resultado resultadoDe(Future<TurnoCreado> futuro) throws InterruptedException {
        try {
            return new Resultado(futuro.get(), null);
        } catch (ExecutionException e) {
            return new Resultado(null, e.getCause());
        }
    }

    @RepeatedTest(10)
    void unProfesionalYUnSillonSoloPermitenUnaReservaSimultanea() throws Exception {
        long prof = profesional();
        escenario.sillon();
        var solicitud = new SolicitudDeReservaDeHueco(
                paciente, prestacion, prof, inicio, OrigenTurno.ONLINE, Actor.paciente());

        List<Resultado> resultados = competir(() -> reserva.reservar(solicitud), () -> reserva.reservar(solicitud));

        assertThat(resultados.stream().filter(Resultado::fueExitosa).count()).isEqualTo(1L);
        assertThat(resultados.stream().filter(r -> !r.fueExitosa()).map(Resultado::error))
                .singleElement()
                .isInstanceOf(HuecoNoDisponibleException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE profesional_id = ?", Long.class, prof))
                .isEqualTo(1L);
    }

    @RepeatedTest(10)
    void cualquieraConDosProfesionalesYDosSillonesAsignaRecursosDistintos() throws Exception {
        long profA = profesional();
        long profB = profesional();
        escenario.sillon();
        escenario.sillon();
        var solicitud = new SolicitudDeReservaDeHueco(
                paciente, prestacion, null, inicio, OrigenTurno.ONLINE, Actor.paciente());

        List<Resultado> resultados = competir(() -> reserva.reservar(solicitud), () -> reserva.reservar(solicitud));

        assertThat(resultados).allMatch(Resultado::fueExitosa);
        TurnoCreado primero = resultados.get(0).creado();
        TurnoCreado segundo = resultados.get(1).creado();
        assertThat(primero.turno().getProfesionalId()).isNotEqualTo(segundo.turno().getProfesionalId());
        assertThat(primero.turno().getSillonId()).isNotEqualTo(segundo.turno().getSillonId());
    }
}

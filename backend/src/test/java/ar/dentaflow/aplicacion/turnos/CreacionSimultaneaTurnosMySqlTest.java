package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec turnos — "Reservas simultáneas sin doble ocupación" y "Bloqueo de recursos en orden fijo" contra MySQL 8
 * real (perfil {@code it-mysql}, design D5 y D11). H2 no reproduce el bloqueo de filas de InnoDB: solo esta prueba
 * demuestra la garantía. No es transaccional: hace commit y borra al terminar todo lo que creó. Cada caso corre 10
 * veces con dos hilos que salen a la vez ({@link CountDownLatch}).
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class CreacionSimultaneaTurnosMySqlTest {

    @Autowired
    private ServicioDeTurnos servicio;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private Instant inicio;
    private long paciente;
    private long prestacion;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        inicio = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
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

    private long sillon() {
        long id = escenario.sillon();
        return id;
    }

    private SolicitudDeTurno solicitud(long profesionalId, long sillonId, Instant desde) {
        return new SolicitudDeTurno(paciente, profesionalId, sillonId, prestacion, desde, OrigenTurno.ONLINE, false,
                Actor.paciente());
    }

    /** Resultado de una tarea concurrente: el valor devuelto o la excepción que lanzó. */
    private record Resultado(Object valor, Throwable error) {
        boolean fueExitosa() {
            return error == null;
        }
    }

    private List<Resultado> competir(Callable<Object> primera, Callable<Object> segunda) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Object> a = hilos.submit(() -> {
                salida.await();
                return primera.call();
            });
            Future<Object> b = hilos.submit(() -> {
                salida.await();
                return segunda.call();
            });
            salida.countDown();
            return List.of(resultadoDe(a), resultadoDe(b));
        } finally {
            hilos.shutdownNow();
        }
    }

    private static Resultado resultadoDe(Future<Object> tarea) throws Exception {
        try {
            return new Resultado(tarea.get(30, TimeUnit.SECONDS), null);
        } catch (ExecutionException e) {
            return new Resultado(null, e.getCause());
        }
    }

    private long turnosActivosDelProfesional(long profesionalId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE profesional_id = ? AND estado <> 'cancelado'",
                Long.class, profesionalId);
    }

    private long turnosActivosDelSillon(long sillonId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE sillon_id = ? AND estado <> 'cancelado'",
                Long.class, sillonId);
    }

    /** Exactamente una ganó y la otra falló por solapamiento (no por interbloqueo ni por tiempo de espera). */
    private static void assertUnaGanaYLaOtraSolapa(List<Resultado> resultados) {
        assertThat(resultados).filteredOn(Resultado::fueExitosa).hasSize(1);
        assertThat(resultados).filteredOn(r -> !r.fueExitosa()).singleElement()
                .satisfies(r -> assertThat(r.error()).isInstanceOf(TurnoSolapadoException.class));
    }

    @RepeatedTest(10)
    void mismoProfesionalYHorarioEnDosSillonesUnaSolaSeConfirma() throws Exception {
        long profesional = profesional();
        long sillon1 = sillon();
        long sillon2 = sillon();

        List<Resultado> resultados = competir(() -> servicio.crear(solicitud(profesional, sillon1, inicio)),
                () -> servicio.crear(solicitud(profesional, sillon2, inicio)));

        assertUnaGanaYLaOtraSolapa(resultados);
        assertThat(turnosActivosDelProfesional(profesional)).isEqualTo(1L);
    }

    @RepeatedTest(10)
    void mismoSillonYHorarioConDosProfesionalesUnaSolaGana() throws Exception {
        long profesional1 = profesional();
        long profesional2 = profesional();
        long sillon = sillon();

        List<Resultado> resultados = competir(() -> servicio.crear(solicitud(profesional1, sillon, inicio)),
                () -> servicio.crear(solicitud(profesional2, sillon, inicio)));

        assertUnaGanaYLaOtraSolapa(resultados);
        assertThat(turnosActivosDelSillon(sillon)).isEqualTo(1L);
    }

    @RepeatedTest(10)
    void recursosDistintosEnElMismoHorarioSeConfirmanAmbas() throws Exception {
        long profesional1 = profesional();
        long profesional2 = profesional();
        long sillon1 = sillon();
        long sillon2 = sillon();

        List<Resultado> resultados = competir(() -> servicio.crear(solicitud(profesional1, sillon1, inicio)),
                () -> servicio.crear(solicitud(profesional2, sillon2, inicio)));

        assertThat(resultados).allMatch(Resultado::fueExitosa);
        assertThat(turnosActivosDelProfesional(profesional1)).isEqualTo(1L);
        assertThat(turnosActivosDelProfesional(profesional2)).isEqualTo(1L);
    }

    @RepeatedTest(10)
    void profesionalesDistintosQueComparteSillonSuperpuestosNoSeInterbloquean() throws Exception {
        long profesional1 = profesional();
        long profesional2 = profesional();
        long sillon2 = sillon();

        List<Resultado> resultados = competir(() -> servicio.crear(solicitud(profesional1, sillon2, inicio)),
                () -> servicio.crear(solicitud(profesional2, sillon2, inicio.plus(15, ChronoUnit.MINUTES))));

        assertUnaGanaYLaOtraSolapa(resultados);
        assertThat(turnosActivosDelSillon(sillon2)).isEqualTo(1L);
    }

    @RepeatedTest(10)
    void unaReprogramacionYUnaCreacionCompitenPorElMismoHuecoYSoloUnaGana() throws Exception {
        long profesional = profesional();
        long sillon1 = sillon();
        long sillon2 = sillon();
        Instant hueco = inicio.plus(2, ChronoUnit.HOURS);
        long usuario = escenario.usuario();
        long turnoAMover = servicio.crear(solicitud(profesional, sillon1, inicio)).turno().getId();

        List<Resultado> resultados = competir(
                () -> servicio.reprogramar(turnoAMover, new SolicitudDeReprogramacion(hueco, profesional, sillon1),
                        Actor.usuario(usuario)),
                () -> servicio.crear(solicitud(profesional, sillon2, hueco)));

        assertUnaGanaYLaOtraSolapa(resultados);
        // Si ganó la reprogramación el turno original se mudó al hueco (1 turno); si ganó la creación el original
        // conserva su horario y hay uno nuevo en el hueco (2 turnos). Nunca dos en el hueco.
        long esperados = resultados.get(0).fueExitosa() ? 1L : 2L;
        assertThat(turnosActivosDelProfesional(profesional)).isEqualTo(esperados);
    }
}

package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec carga-manual-turnos — "Cargas simultáneas sin doble ocupación" contra MySQL 8 real (perfil {@code it-mysql},
 * C-12, design D7). El bloqueo de recursos ya lo cubre {@code CreacionSimultaneaTurnosMySqlTest}; acá se verifica el
 * camino nuevo de punta a punta: cada carga registra a su paciente en otra transacción y después crea el turno. No es
 * transaccional: hace commit y borra al terminar todo lo que creó. Dos hilos salen a la vez ({@link CountDownLatch}).
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
@DisplayName("Cargas manuales simultáneas (MySQL 8)")
class CargaManualSimultaneaMySqlTest {

    @Autowired
    private CargaManualDeTurnos carga;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private final List<String> dnisCreados = new ArrayList<>();
    private long profesional;
    private long sillon1;
    private long sillon2;
    private long prestacion;
    private long usuario;
    private Instant inicio;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        prestacion = escenario.prestacion(30);
        profesional = escenario.profesional();
        escenario.habilitar(profesional, prestacion);
        escenario.atiendeTodaLaSemana(profesional);
        sillon1 = escenario.sillon();
        sillon2 = escenario.sillon();
        usuario = escenario.usuario();
        // 10:00 del consultorio dentro de 30 días: siempre dentro del horario 00:00-23:59 del profesional.
        inicio = ZonaHorariaConsultorio.aInstante(
                LocalDateTime.of(ZonaHorariaConsultorio.hoy(java.time.Clock.systemUTC()).plusDays(30), LocalTime.of(10, 0)));
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
        dnisCreados.forEach(dni -> jdbc.update("DELETE FROM paciente WHERE dni = ?", dni));
    }

    private PedidoDeCargaManual pedido(long sillon) {
        String dni = escenario.dniNuevo();
        dnisCreados.add(dni);
        return new PedidoDeCargaManual(dni, "Ana", "Ejemplo", "+541155550000", "ana-" + dni + "@example.com",
                prestacion, profesional, sillon, inicio);
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

    @RepeatedTest(5)
    @DisplayName("mismo profesional y horario en sillones distintos: una carga gana y la otra recibe el conflicto")
    void dosCargasDelMismoProfesionalYHorario() throws Exception {
        PedidoDeCargaManual primero = pedido(sillon1);
        PedidoDeCargaManual segundo = pedido(sillon2);

        List<Resultado> resultados = competir(() -> carga.cargar(primero, usuario),
                () -> carga.cargar(segundo, usuario));

        assertThat(resultados).filteredOn(Resultado::fueExitosa).hasSize(1);
        assertThat(resultados).filteredOn(r -> !r.fueExitosa()).singleElement()
                .satisfies(r -> assertThat(r.error()).isInstanceOf(ConflictoException.class)
                        .isInstanceOf(ConflictoConAlternativasException.class));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE profesional_id = ? AND estado <> 'cancelado'",
                Long.class, profesional)).isEqualTo(1L);
        // Ambos pacientes quedaron registrados aunque uno de los turnos se rechazó (contrato de C-09).
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni IN (?, ?)", Long.class,
                primero.dni(), segundo.dni())).isEqualTo(2L);
    }
}

package ar.dentaflow.aplicacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec horarios-atencion — "Altas simultáneas sin superposición", contra MySQL 8 real (perfil
 * {@code it-mysql}). Dos hilos crean a la vez rangos superpuestos del mismo profesional: el bloqueo
 * pesimista de la fila del profesional (design D6) serializa las dos transacciones y exactamente una
 * gana. No es transaccional: hace commit, así que borra en {@code @AfterEach} lo que creó.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class AltaSimultaneaHorariosMySqlTest {

    @Autowired
    private ServicioDeHorarios servicio;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private final List<Long> creados = new ArrayList<>();

    @AfterEach
    void limpiar() {
        for (Long id : creados) {
            jdbc.update("DELETE FROM horario_atencion WHERE profesional_id = ?", id);
            jdbc.update("DELETE FROM profesional WHERE id = ?", id);
        }
    }

    private Long profesional(String nombre) {
        Long id = profesionales.guardar(Profesional.nuevo(nombre, "Simultaneo", null,
                nombre.toLowerCase() + "-simultaneo-" + sufijo + "@example.com")).getId();
        creados.add(id);
        return id;
    }

    /** Lanza las dos altas a la vez y devuelve el error de cada una ({@code null} si terminó bien). */
    private List<Throwable> altasALaVez(Long profesionalA, LocalTime inicioA, LocalTime finA,
            Long profesionalB, LocalTime inicioB, LocalTime finB) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = hilos.submit(() -> {
                salida.await();
                return servicio.crear(profesionalA, DayOfWeek.MONDAY, inicioA, finA);
            });
            Future<?> b = hilos.submit(() -> {
                salida.await();
                return servicio.crear(profesionalB, DayOfWeek.MONDAY, inicioB, finB);
            });
            salida.countDown();
            List<Throwable> errores = new ArrayList<>();
            errores.add(error(a));
            errores.add(error(b));
            return errores;
        } finally {
            hilos.shutdownNow();
        }
    }

    private static Throwable error(Future<?> alta) throws Exception {
        try {
            alta.get(30, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException e) {
            return e.getCause();
        }
    }

    private int rangos(Long profesionalId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM horario_atencion WHERE profesional_id = ?",
                Integer.class, profesionalId);
    }

    @RepeatedTest(10)
    void dosAltasSuperpuestasDelMismoProfesionalYGanaExactamenteUna() throws Exception {
        Long id = profesional("Laura");

        List<Throwable> errores = altasALaVez(id, LocalTime.of(9, 0), LocalTime.of(13, 0),
                id, LocalTime.of(12, 0), LocalTime.of(16, 0));

        assertThat(errores).filteredOn(e -> e == null).hasSize(1);
        assertThat(errores).filteredOn(e -> e != null).singleElement().isInstanceOf(ConflictoException.class);
        assertThat(rangos(id)).isEqualTo(1);
    }

    @RepeatedTest(10)
    void conDosProfesionalesDistintosAmbasAltasTerminanBien() throws Exception {
        Long a = profesional("Martin");
        Long b = profesional("Sofia");

        List<Throwable> errores = altasALaVez(a, LocalTime.of(9, 0), LocalTime.of(13, 0),
                b, LocalTime.of(12, 0), LocalTime.of(16, 0));

        assertThat(errores).containsOnlyNulls();
        assertThat(rangos(a)).isEqualTo(1);
        assertThat(rangos(b)).isEqualTo(1);
    }
}

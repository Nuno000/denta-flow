package ar.dentaflow.aplicacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.pacientes.Paciente;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec pacientes — "Dos altas simultáneas del mismo DNI" contra MySQL 8 real (perfil {@code it-mysql}). No es
 * transaccional: hace commit, así que borra por DNI ficticio al terminar. Pacientes sin turnos, se pueden borrar.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class AltaSimultaneaPacienteMySqlTest {

    @Autowired
    private ServicioDePacientes servicio;

    @Autowired
    private JdbcTemplate jdbc;

    private String dni;

    @AfterEach
    void limpiar() {
        if (dni != null) {
            jdbc.update("DELETE FROM paciente WHERE dni = ?", dni);
        }
    }

    @RepeatedTest(10)
    void dosAltasDelMismoDniDevuelvenAlMismoPacienteYQuedaUnaFila() throws Exception {
        dni = Long.toString(ThreadLocalRandom.current().nextLong(10_000_000L, 100_000_000L));
        jdbc.update("DELETE FROM paciente WHERE dni = ?", dni);
        DatosPaciente datos = new DatosPaciente(dni, "Ana", "Ejemplo", "1155550000", "simultaneo@example.com", null);
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Paciente> a = hilos.submit(() -> {
                salida.await();
                return servicio.registrarDesdeReservaOnline(datos);
            });
            Future<Paciente> b = hilos.submit(() -> {
                salida.await();
                return servicio.registrarDesdeReservaOnline(datos);
            });
            salida.countDown();

            Long idA = a.get(30, TimeUnit.SECONDS).getId();
            Long idB = b.get(30, TimeUnit.SECONDS).getId();

            assertThat(idA).isEqualTo(idB);
        } finally {
            hilos.shutdownNow();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM paciente WHERE dni = ?", Long.class, dni))
                .isEqualTo(1L);
    }
}

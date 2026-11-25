package ar.dentaflow.aplicacion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.usuarios.Rol;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
 * Spec parametros-consultorio — "Ediciones simultáneas sin valores mezclados", contra MySQL 8 real
 * (perfil {@code it-mysql}). No es transaccional: hace commit, así que restaura los valores iniciales y
 * borra sus usuarios al terminar.
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class EdicionSimultaneaParametrosMySqlTest {

    private static final String CLAVE = "anticipacion-minima-horas";

    @Autowired
    private ServicioDeParametros servicio;

    @Autowired
    private ServicioDeUsuarios usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);
    private Long primero;
    private Long segundo;

    @AfterEach
    void restaurar() {
        jdbc.update("UPDATE parametro SET valor = '24', provisorio = TRUE, actualizado_en = NULL, "
                + "actualizado_por = NULL WHERE clave = ?", CLAVE);
        jdbc.update("DELETE FROM usuario WHERE email LIKE ?", "simultaneo-%-" + sufijo + "@example.com");
    }

    private Long administrador(String base) {
        return usuarios.crear("simultaneo-" + base + "-" + sufijo + "@example.com", "contrasena-valida",
                Rol.ADMINISTRADOR, null).usuario().getId();
    }

    @RepeatedTest(10)
    void dosAdministradoresGuardanALaVezYQuedaUnaEdicionCompleta() throws Exception {
        primero = administrador("a");
        segundo = administrador("b");
        CountDownLatch salida = new CountDownLatch(1);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<?> a = hilos.submit(() -> {
                salida.await();
                return servicio.actualizar(CLAVE, "48", primero);
            });
            Future<?> b = hilos.submit(() -> {
                salida.await();
                return servicio.actualizar(CLAVE, "72", segundo);
            });
            salida.countDown();
            a.get(30, TimeUnit.SECONDS);
            b.get(30, TimeUnit.SECONDS);
        } finally {
            hilos.shutdownNow();
        }

        List<Map<String, Object>> filas = jdbc.queryForList(
                "SELECT valor, provisorio, actualizado_por, actualizado_en FROM parametro WHERE clave = ?", CLAVE);
        assertThat(filas).hasSize(1);
        Map<String, Object> fila = filas.get(0);
        String valor = (String) fila.get("valor");
        long autor = ((Number) fila.get("actualizado_por")).longValue();
        assertThat(fila.get("actualizado_en")).isNotNull();
        assertThat(((Boolean) fila.get("provisorio"))).isFalse();
        assertThat(valor.equals("48") && autor == primero || valor.equals("72") && autor == segundo)
                .as("valor %s con autor %s", valor, autor).isTrue();
    }
}

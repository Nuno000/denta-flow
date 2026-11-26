package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Spec turnos — "Única vía de escritura de turnos" (RN-GL-02, regla dura de CLAUDE.md): recorre {@code src/main/java}
 * (como {@code DominioSinFrameworksTest}) y exige que nadie fuera de {@code ServicioDeTurnos} y de
 * {@code infraestructura/persistencia/turnos/} escriba turnos: ni llame a {@code TurnoRepositorio.guardar} ni
 * ejecute {@code INSERT}, {@code UPDATE} o {@code DELETE} sobre la tabla {@code turno}.
 */
class UnicaViaDeEscrituraDeTurnosTest {

    private static final Path FUENTES = Path.of("src/main/java");
    private static final String SERVICIO = "ar/dentaflow/aplicacion/turnos/ServicioDeTurnos.java";
    private static final String PERSISTENCIA = "ar/dentaflow/infraestructura/persistencia/turnos/";
    /** El puerto, su implementación y el servicio de turnos del dominio (el puerto declara guardar). */
    private static final String PUERTO = "ar/dentaflow/dominio/turnos/TurnoRepositorio.java";

    private static final Pattern USA_EL_REPOSITORIO_DE_TURNOS = Pattern.compile("\\bTurnoRepositorio(Jpa)?\\b");
    private static final Pattern ESCRIBE_CON_EL_REPOSITORIO = Pattern.compile("\\.(guardar|save)\\(");
    private static final Pattern SQL_DE_ESCRITURA_SOBRE_TURNO = Pattern.compile(
            "(?is)(INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\s+turno\\b");

    /** {@code true} si el código fuente escribe turnos. */
    static boolean escribeTurnos(String fuente) {
        boolean porRepositorio = USA_EL_REPOSITORIO_DE_TURNOS.matcher(fuente).find()
                && ESCRIBE_CON_EL_REPOSITORIO.matcher(fuente).find();
        return porRepositorio || SQL_DE_ESCRITURA_SOBRE_TURNO.matcher(fuente).find();
    }

    @Test
    void soloElServicioDeTurnosYSuPersistenciaEscribenTurnos() throws IOException {
        try (Stream<Path> archivos = Files.walk(FUENTES)) {
            List<String> infractores = archivos
                    .filter(archivo -> archivo.toString().endsWith(".java"))
                    .filter(archivo -> !esDelCaminoPermitido(archivo))
                    .filter(archivo -> escribeTurnos(leer(archivo)))
                    .map(Path::toString)
                    .toList();

            assertThat(infractores).isEmpty();
        }
    }

    @Test
    void elDetectorReconoceLasEscriturasProhibidas() {
        assertThat(escribeTurnos("TurnoRepositorio turnos; ... turnos.guardar(turno);")).isTrue();
        assertThat(escribeTurnos("private TurnoRepositorioJpa repo; repo.save(t);")).isTrue();
        assertThat(escribeTurnos("jdbc.update(\"INSERT INTO turno (id) VALUES (1)\")")).isTrue();
        assertThat(escribeTurnos("jdbc.update(\"update turno set estado = 'x'\")")).isTrue();
        assertThat(escribeTurnos("jdbc.update(\"DELETE FROM turno WHERE id = 1\")")).isTrue();
    }

    @Test
    void elDetectorNoMarcaLecturasNiOtrasTablas() {
        assertThat(escribeTurnos("TurnoRepositorio turnos; turnos.buscarPorId(1L);")).isFalse();
        assertThat(escribeTurnos("PacienteRepositorio p; p.guardar(paciente);")).isFalse();
        assertThat(escribeTurnos("jdbc.update(\"UPDATE turno_historial SET x = 1\")")).isFalse();
        assertThat(escribeTurnos("jdbc.queryForObject(\"SELECT COUNT(*) FROM turno\", Long.class)")).isFalse();
    }

    private static boolean esDelCaminoPermitido(Path archivo) {
        String ruta = archivo.toString().replace('\\', '/');
        return ruta.endsWith(SERVICIO) || ruta.endsWith(PUERTO) || ruta.contains(PERSISTENCIA);
    }

    private static String leer(Path archivo) {
        try {
            return Files.readString(archivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

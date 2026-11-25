package ar.dentaflow.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Regla dura de CLAUDE.md: el paquete dominio no importa Spring ni JPA. */
class DominioSinFrameworksTest {

    private static final Path DOMINIO = Path.of("src/main/java/ar/dentaflow/dominio");
    private static final List<String> PROHIBIDOS =
            List.of("import org.springframework", "import jakarta.persistence", "import org.hibernate");

    @Test
    void ningunArchivoDelDominioImportaFrameworks() throws IOException {
        try (Stream<Path> archivos = Files.walk(DOMINIO)) {
            List<String> infractores = archivos
                    .filter(archivo -> archivo.toString().endsWith(".java"))
                    .filter(DominioSinFrameworksTest::importaFramework)
                    .map(Path::toString)
                    .toList();

            assertThat(infractores).isEmpty();
        }
    }

    private static boolean importaFramework(Path archivo) {
        try {
            String fuente = Files.readString(archivo, StandardCharsets.UTF_8);
            return PROHIBIDOS.stream().anyMatch(fuente::contains);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

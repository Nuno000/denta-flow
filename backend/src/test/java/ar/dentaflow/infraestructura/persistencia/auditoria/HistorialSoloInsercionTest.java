package ar.dentaflow.infraestructura.persistencia.auditoria;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.auditoria.HistorialDeTurnoRepositorio;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Spec "Historial solo de inserción" — escenario "Sin operación de borrado" (design D4). */
class HistorialSoloInsercionTest {

    private static final List<String> PROHIBIDOS =
            List.of("delete", "remove", "update", "actualizar", "borrar", "modificar", "saveall", "flush");

    @Test
    void elPuertoSoloAgregaYConsulta() {
        assertThat(nombres(HistorialDeTurnoRepositorio.class)).containsExactlyInAnyOrder("agregar", "historialDe");
    }

    @Test
    void elRepositorioNoOfreceModificarNiBorrar() {
        assertThat(nombres(HistorialDeTurnoRepositorioJpa.class))
                .isNotEmpty()
                .noneMatch(nombre -> PROHIBIDOS.stream()
                        .anyMatch(prohibido -> nombre.toLowerCase(Locale.ROOT).startsWith(prohibido)));
    }

    private static List<String> nombres(Class<?> tipo) {
        return Arrays.stream(tipo.getMethods()).map(Method::getName).distinct().toList();
    }
}

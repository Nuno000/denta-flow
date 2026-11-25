package ar.dentaflow.infraestructura.persistencia.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

/** Spec recursos-consultorio — "Desactivar sin borrar": ningún repositorio expone borrado. */
class SinBorradoDeRecursosTest {

    private static Stream<Class<?>> repositoriosJpa() {
        return Stream.of(PrestacionRepositorioJpa.class, SillonRepositorioJpa.class, ProfesionalRepositorioJpa.class);
    }

    private static Stream<Class<?>> puertos() {
        return Stream.of(PrestacionRepositorio.class, SillonRepositorio.class, ProfesionalRepositorio.class);
    }

    @Test
    void losRepositoriosSpringDataSoloExtiendenRepositoryYNoCrudRepository() {
        repositoriosJpa().forEach(tipo -> {
            assertThat(Repository.class).isAssignableFrom(tipo);
            assertThat(CrudRepository.class.isAssignableFrom(tipo)).as(tipo.getSimpleName()).isFalse();
        });
    }

    @Test
    void ningunMetodoDeLosRepositoriosNiDeLosPuertosBorra() {
        Stream.concat(repositoriosJpa(), puertos()).forEach(tipo ->
                assertThat(Arrays.stream(tipo.getMethods()).map(m -> m.getName().toLowerCase()))
                        .as(tipo.getSimpleName())
                        .noneMatch(nombre -> nombre.startsWith("delete") || nombre.startsWith("borrar")
                                || nombre.startsWith("eliminar") || nombre.startsWith("remove")));
    }
}

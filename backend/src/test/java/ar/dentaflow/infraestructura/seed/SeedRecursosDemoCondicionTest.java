package ar.dentaflow.infraestructura.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Spec recursos-consultorio — "Datos de demostración": el seed solo existe con {@code app.seed.demo=true}. */
class SeedRecursosDemoCondicionTest {

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withBean(PrestacionRepositorio.class, () -> mock(PrestacionRepositorio.class))
            .withBean(SillonRepositorio.class, () -> mock(SillonRepositorio.class))
            .withBean(ProfesionalRepositorio.class, () -> mock(ProfesionalRepositorio.class))
            .withBean(HorarioAtencionRepositorio.class, () -> mock(HorarioAtencionRepositorio.class))
            .withUserConfiguration(SeedRecursosDemo.class);

    @Test
    void sinLaPropiedadNoExisteElBean() {
        contexto.run(ctx -> assertThat(ctx).doesNotHaveBean(SeedRecursosDemo.class));
    }

    @Test
    void conLaPropiedadEnFalseNoExisteElBean() {
        contexto.withPropertyValues("app.seed.demo=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(SeedRecursosDemo.class));
    }

    @Test
    void conLaPropiedadEnTrueExisteElBean() {
        contexto.withPropertyValues("app.seed.demo=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(SeedRecursosDemo.class));
    }
}

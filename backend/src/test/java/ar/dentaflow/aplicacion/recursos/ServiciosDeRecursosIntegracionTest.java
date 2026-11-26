package ar.dentaflow.aplicacion.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec gestion-recursos — alta, edición, asignación y respaldo de duplicados contra H2 {@code MODE=MySQL}
 * (los casos de respaldo vienen de {@link RespaldoDuplicadosRecursosBase}).
 */
@SpringBootTest
@ActiveProfiles("test")
class ServiciosDeRecursosIntegracionTest extends RespaldoDuplicadosRecursosBase {

    @Autowired
    private ServicioDeSillones sillones;

    @Autowired
    private ServicioDePrestaciones prestaciones;

    @Autowired
    private ServicioDeProfesionales profesionales;

    @Autowired
    private ProfesionalRepositorio profesionalRepositorio;

    @Autowired
    private EntityManager em;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    @Test
    void altaEdicionYRelecturaDeLosTresRecursos() {
        Sillon sillon = sillones.crear("Box " + sufijo);
        Prestacion prestacion = prestaciones.crear("Control " + sufijo, 20);
        Profesional profesional = profesionales.crear("Ana", "Muestra", null, "ana-" + sufijo + "@example.com");

        sillones.renombrar(sillon.getId(), "Box nuevo " + sufijo);
        prestaciones.actualizar(prestacion.getId(), "Control " + sufijo, 25);
        profesionales.actualizar(profesional.getId(), "Ana María", "Muestra", "MP-" + sufijo,
                "ana-" + sufijo + "@example.com");
        em.flush();
        em.clear();

        assertThat(sillones.obtener(sillon.getId()).getNombre()).isEqualTo("Box nuevo " + sufijo);
        assertThat(prestaciones.obtener(prestacion.getId()).getDuracionMinutos()).isEqualTo(25);
        Profesional releido = profesionales.obtener(profesional.getId());
        assertThat(releido.getNombre()).isEqualTo("Ana María");
        assertThat(releido.getMatricula()).isEqualTo("MP-" + sufijo);
    }

    @Test
    void asignarPrestacionesYVaciarlasActualizaLosCandidatos() {
        Prestacion prestacion = prestaciones.crear("Ortodoncia " + sufijo, 30);
        Profesional profesional = profesionales.crear("Ana", "Muestra", null, "ana-" + sufijo + "@example.com");

        profesionales.asignarPrestaciones(profesional.getId(), List.of(prestacion.getId()));
        em.flush();
        em.clear();
        assertThat(profesionalRepositorio.listarActivosQueRealizan(prestacion.getId()))
                .extracting(Profesional::getId).containsExactly(profesional.getId());

        profesionales.asignarPrestaciones(profesional.getId(), List.of());
        em.flush();
        em.clear();
        assertThat(profesionalRepositorio.listarActivosQueRealizan(prestacion.getId())).isEmpty();
    }

    @Test
    void desactivarUnaPrestacionConservaLaAsociacion() {
        Prestacion prestacion = prestaciones.crear("Implante " + sufijo, 90);
        Profesional profesional = profesionales.crear("Ana", "Muestra", null, "ana-" + sufijo + "@example.com");
        profesionales.asignarPrestaciones(profesional.getId(), List.of(prestacion.getId()));

        prestaciones.desactivar(prestacion.getId());
        em.flush();
        em.clear();

        assertThat(profesionales.obtener(profesional.getId()).prestacionIds()).containsExactly(prestacion.getId());
        assertThat(prestaciones.obtener(prestacion.getId()).isActiva()).isFalse();
    }
}

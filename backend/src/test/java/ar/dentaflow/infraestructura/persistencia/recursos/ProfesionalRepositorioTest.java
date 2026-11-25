package ar.dentaflow.infraestructura.persistencia.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec recursos-consultorio — "Prestaciones que realiza cada profesional", "Desactivar sin borrar"
 * y "Profesionales activos por prestación".
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProfesionalRepositorioTest {

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private PrestacionRepositorio prestaciones;

    @Autowired
    private EntityManager em;

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    private Prestacion prestacion(String nombre) {
        return prestaciones.guardar(Prestacion.nueva(nombre, 30));
    }

    private Profesional profesional(String nombre, String apellido, Prestacion... habilitadas) {
        Profesional profesional = Profesional.nuevo(nombre, apellido, null,
                nombre.toLowerCase() + "." + apellido.toLowerCase() + "@example.com");
        for (Prestacion habilitada : habilitadas) {
            profesional.habilitarPrestacion(habilitada);
        }
        return profesionales.guardar(profesional);
    }

    @Test
    void idaYVueltaSinMatriculaYConPrestaciones() {
        Prestacion limpieza = prestacion("Limpieza");
        Prestacion consulta = prestacion("Consulta");
        Profesional guardado = profesional("Martín", "Prueba", limpieza, consulta);
        sincronizar();

        Profesional leido = profesionales.buscarPorId(guardado.getId()).orElseThrow();

        assertThat(leido.getNombre()).isEqualTo("Martín");
        assertThat(leido.getApellido()).isEqualTo("Prueba");
        assertThat(leido.getMatricula()).isNull();
        assertThat(leido.getEmail()).isEqualTo("martín.prueba@example.com");
        assertThat(leido.isActivo()).isTrue();
        assertThat(leido.prestacionIds()).containsExactlyInAnyOrder(limpieza.getId(), consulta.getId());
    }

    @Test
    void idaYVueltaConMatricula() {
        Profesional guardado = profesionales.guardar(
                Profesional.nuevo("Laura", "Ejemplo", "MP-0001", "laura@example.com"));
        sincronizar();

        assertThat(profesionales.buscarPorId(guardado.getId()).orElseThrow().getMatricula()).isEqualTo("MP-0001");
        assertThat(profesionales.buscarPorId(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void quitarUnaPrestacionYGuardarBorraSoloEsaAsociacion() {
        Prestacion limpieza = prestacion("Limpieza");
        Prestacion consulta = prestacion("Consulta");
        Profesional guardado = profesional("Laura", "Ejemplo", limpieza, consulta);
        sincronizar();

        Profesional leido = profesionales.buscarPorId(guardado.getId()).orElseThrow();
        leido.deshabilitarPrestacion(limpieza.getId());
        profesionales.guardar(leido);
        sincronizar();

        assertThat(profesionales.buscarPorId(guardado.getId()).orElseThrow().prestacionIds())
                .containsExactly(consulta.getId());
        assertThat(prestaciones.buscarPorId(limpieza.getId())).isPresent();
    }

    @Test
    void desactivarYGuardarConservaLasPrestaciones() {
        Prestacion limpieza = prestacion("Limpieza");
        Profesional guardado = profesional("Laura", "Ejemplo", limpieza);
        sincronizar();

        Profesional leido = profesionales.buscarPorId(guardado.getId()).orElseThrow();
        leido.desactivar();
        profesionales.guardar(leido);
        sincronizar();

        Profesional releido = profesionales.buscarPorId(guardado.getId()).orElseThrow();
        assertThat(releido.isActivo()).isFalse();
        assertThat(releido.prestacionIds()).containsExactly(limpieza.getId());
    }

    @Test
    void listarActivosOrdenaPorApellidoYNombreYExcluyeInactivos() {
        profesional("Zoe", "Beta");
        profesional("Ana", "Beta");
        profesional("Carlos", "Alfa");
        Profesional inactivo = profesional("Bruno", "Alfa");
        inactivo.desactivar();
        profesionales.guardar(inactivo);
        sincronizar();

        assertThat(profesionales.listarActivos()).extracting(Profesional::getNombre)
                .containsExactly("Carlos", "Ana", "Zoe");
        assertThat(profesionales.listarTodos()).extracting(Profesional::getNombre)
                .containsExactly("Bruno", "Carlos", "Ana", "Zoe");
    }

    @Test
    void listarActivosQueRealizanDevuelveSoloLosActivosQueLaRealizanPorId() {
        Prestacion limpieza = prestacion("Limpieza");
        Prestacion extraccion = prestacion("Extracción");
        Profesional primero = profesional("Ana", "Uno", limpieza);
        Profesional inactivo = profesional("Bruno", "Dos", limpieza);
        inactivo.desactivar();
        profesionales.guardar(inactivo);
        Profesional noLaRealiza = profesional("Carla", "Tres", extraccion);
        Profesional segundo = profesional("Dora", "Cuatro", limpieza, extraccion);
        sincronizar();

        assertThat(profesionales.listarActivosQueRealizan(limpieza.getId())).extracting(Profesional::getId)
                .containsExactly(primero.getId(), segundo.getId());
        assertThat(profesionales.listarActivosQueRealizan(extraccion.getId())).extracting(Profesional::getId)
                .containsExactly(noLaRealiza.getId(), segundo.getId());
        assertThat(profesionales.listarActivosQueRealizan(Long.MAX_VALUE)).isEmpty();
    }
}

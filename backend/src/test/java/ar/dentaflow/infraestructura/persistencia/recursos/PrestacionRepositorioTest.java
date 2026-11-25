package ar.dentaflow.infraestructura.persistencia.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Spec recursos-consultorio — "Desactivar sin borrar" y duración de la prestación, sobre H2 modo MySQL. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PrestacionRepositorioTest {

    @Autowired
    private PrestacionRepositorio prestaciones;

    @Autowired
    private EntityManager em;

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    @Test
    void guardaYRecuperaPorIdConTodosSusCampos() {
        Prestacion guardada = prestaciones.guardar(Prestacion.nueva("Limpieza", 40));
        sincronizar();

        Prestacion leida = prestaciones.buscarPorId(guardada.getId()).orElseThrow();

        assertThat(guardada.getId()).isNotNull();
        assertThat(leida.getNombre()).isEqualTo("Limpieza");
        assertThat(leida.getDuracionMinutos()).isEqualTo(40);
        assertThat(leida.isActiva()).isTrue();
    }

    @Test
    void buscarPorIdInexistenteDevuelveVacio() {
        assertThat(prestaciones.buscarPorId(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void listarActivasExcluyeLaDesactivadaYOrdenaPorNombre() {
        prestaciones.guardar(Prestacion.nueva("Zeta", 30));
        prestaciones.guardar(Prestacion.nueva("Alfa", 20));
        Prestacion inactiva = Prestacion.nueva("Medio", 10);
        inactiva.desactivar();
        prestaciones.guardar(inactiva);
        sincronizar();

        assertThat(prestaciones.listarActivas()).extracting(Prestacion::getNombre).containsExactly("Alfa", "Zeta");
        assertThat(prestaciones.listarTodas()).extracting(Prestacion::getNombre)
                .containsExactly("Alfa", "Medio", "Zeta");
    }

    @Test
    void desactivarYGuardarConservaLaFila() {
        Prestacion guardada = prestaciones.guardar(Prestacion.nueva("Limpieza", 40));
        sincronizar();

        Prestacion leida = prestaciones.buscarPorId(guardada.getId()).orElseThrow();
        leida.desactivar();
        prestaciones.guardar(leida);
        sincronizar();

        Prestacion releida = prestaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertThat(releida.isActiva()).isFalse();
        assertThat(releida.getDuracionMinutos()).isEqualTo(40);
        assertThat(prestaciones.listarTodas()).extracting(Prestacion::getId).contains(guardada.getId());
        assertThat(prestaciones.listarActivas()).extracting(Prestacion::getId).doesNotContain(guardada.getId());
    }

    @Test
    void cambiarDuracionSePersiste() {
        Prestacion guardada = prestaciones.guardar(Prestacion.nueva("Limpieza", 40));
        guardada.cambiarDuracion(55);
        prestaciones.guardar(guardada);
        sincronizar();

        assertThat(prestaciones.buscarPorId(guardada.getId()).orElseThrow().getDuracionMinutos()).isEqualTo(55);
    }
}

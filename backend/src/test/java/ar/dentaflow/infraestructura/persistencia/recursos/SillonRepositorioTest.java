package ar.dentaflow.infraestructura.persistencia.recursos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/** Spec recursos-consultorio — "Identificación del sillón" y "Desactivar sin borrar". */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SillonRepositorioTest {

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private EntityManager em;

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    @Test
    void guardaYRecuperaPorId() {
        Sillon guardado = sillones.guardar(Sillon.nuevo("Sillón 1"));
        sincronizar();

        Sillon leido = sillones.buscarPorId(guardado.getId()).orElseThrow();

        assertThat(guardado.getId()).isNotNull();
        assertThat(leido.getNombre()).isEqualTo("Sillón 1");
        assertThat(leido.isActivo()).isTrue();
        assertThat(sillones.buscarPorId(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void listarActivosExcluyeLosInactivosYOrdenaPorIdAscendente() {
        Sillon segundo = sillones.guardar(Sillon.nuevo("B"));
        Sillon inactivo = Sillon.nuevo("A");
        inactivo.desactivar();
        sillones.guardar(inactivo);
        Sillon tercero = sillones.guardar(Sillon.nuevo("A-bis"));
        sincronizar();

        assertThat(sillones.listarActivos()).extracting(Sillon::getId)
                .containsExactly(segundo.getId(), tercero.getId());
        assertThat(sillones.listarTodos()).extracting(Sillon::getNombre).containsExactly("B", "A", "A-bis");
    }

    @Test
    void desactivarYGuardarConservaLaFila() {
        Sillon guardado = sillones.guardar(Sillon.nuevo("Sillón 1"));
        sincronizar();

        Sillon leido = sillones.buscarPorId(guardado.getId()).orElseThrow();
        leido.desactivar();
        sillones.guardar(leido);
        sincronizar();

        assertThat(sillones.buscarPorId(guardado.getId()).orElseThrow().isActivo()).isFalse();
        assertThat(sillones.listarTodos()).extracting(Sillon::getId).contains(guardado.getId());
        assertThat(sillones.listarActivos()).extracting(Sillon::getId).doesNotContain(guardado.getId());
    }

    @Test
    void renombrarSePersiste() {
        Sillon guardado = sillones.guardar(Sillon.nuevo("Sillón 1"));
        guardado.renombrar("Sillón principal");
        sillones.guardar(guardado);
        sincronizar();

        assertThat(sillones.buscarPorId(guardado.getId()).orElseThrow().getNombre()).isEqualTo("Sillón principal");
    }
}

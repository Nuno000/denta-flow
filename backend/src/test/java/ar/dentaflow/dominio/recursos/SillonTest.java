package ar.dentaflow.dominio.recursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec recursos-consultorio — "Identificación del sillón", "Desactivar sin borrar". */
class SillonTest {

    @Test
    void elSillonNuevoQuedaActivoConSuNombre() {
        Sillon sillon = Sillon.nuevo("Sillón 1");

        assertThat(sillon.getNombre()).isEqualTo("Sillón 1");
        assertThat(sillon.isActivo()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "    "})
    void rechazaNombreNuloOEnBlanco(String nombre) {
        assertThatThrownBy(() -> Sillon.nuevo(nombre))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void aceptaCincuentaCaracteresYRechazaMas() {
        assertThat(Sillon.nuevo("s".repeat(50)).getNombre()).hasSize(50);
        assertThatThrownBy(() -> Sillon.nuevo("s".repeat(51)))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("50");
    }

    @Test
    void renombrarAplicaLaMismaValidacionYNoCambiaSiFalla() {
        Sillon sillon = Sillon.nuevo("Sillón 1");

        sillon.renombrar("  Sillón 2 ");
        assertThat(sillon.getNombre()).isEqualTo("Sillón 2");

        assertThatThrownBy(() -> sillon.renombrar(" "))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(sillon.getNombre()).isEqualTo("Sillón 2");
    }

    @Test
    void desactivarYActivarSonIdempotentes() {
        Sillon sillon = Sillon.nuevo("Sillón 1");

        sillon.desactivar();
        sillon.desactivar();
        assertThat(sillon.isActivo()).isFalse();
        assertThat(sillon.getNombre()).isEqualTo("Sillón 1");

        sillon.activar();
        sillon.activar();
        assertThat(sillon.isActivo()).isTrue();
    }

    @Test
    void igualdadPorIdYSinIdSoloIgualASiMismo() {
        Sillon a = AyudanteDeIds.conId(Sillon.nuevo("A"), 1L);
        Sillon mismoId = AyudanteDeIds.conId(Sillon.nuevo("B"), 1L);
        Sillon otroId = AyudanteDeIds.conId(Sillon.nuevo("A"), 2L);
        Sillon sinId = Sillon.nuevo("A");

        assertThat(a).isEqualTo(mismoId).hasSameHashCodeAs(mismoId).isNotEqualTo(otroId);
        assertThat(sinId).isEqualTo(sinId).isNotEqualTo(Sillon.nuevo("A"));
    }
}

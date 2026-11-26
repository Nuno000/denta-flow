package ar.dentaflow.dominio.recursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec recursos-consultorio — "Duración obligatoria", "Datos obligatorios", "Desactivar sin borrar". */
class PrestacionTest {

    @Test
    void laPrestacionNuevaQuedaActivaConSuDuracion() {
        Prestacion limpieza = Prestacion.nueva("Limpieza", 40);

        assertThat(limpieza.getNombre()).isEqualTo("Limpieza");
        assertThat(limpieza.getDuracionMinutos()).isEqualTo(40);
        assertThat(limpieza.isActiva()).isTrue();
    }

    @Test
    void aceptaCualquierDuracionPositiva() {
        assertThat(Prestacion.nueva("Control", 1).getDuracionMinutos()).isEqualTo(1);
        assertThat(Prestacion.nueva("Cirugía", 600).getDuracionMinutos()).isEqualTo(600);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -15})
    void rechazaDuracionNulaCeroONegativa(Integer duracion) {
        assertThatThrownBy(() -> Prestacion.nueva("Limpieza", duracion))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("duración");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rechazaNombreNuloOEnBlanco(String nombre) {
        assertThatThrownBy(() -> Prestacion.nueva(nombre, 30))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    void recortaElNombre() {
        assertThat(Prestacion.nueva("  Limpieza  ", 30).getNombre()).isEqualTo("Limpieza");
    }

    @Test
    void aceptaNombreDeCienCaracteresYRechazaMas() {
        assertThat(Prestacion.nueva("a".repeat(100), 30).getNombre()).hasSize(100);
        assertThatThrownBy(() -> Prestacion.nueva("a".repeat(101), 30))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("100");
    }

    @Test
    void cambiarDuracionAplicaLaNuevaDuracion() {
        Prestacion prestacion = Prestacion.nueva("Limpieza", 40);

        prestacion.cambiarDuracion(55);

        assertThat(prestacion.getDuracionMinutos()).isEqualTo(55);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void cambiarDuracionInvalidaNoModificaLaPrestacion(Integer duracion) {
        Prestacion prestacion = Prestacion.nueva("Limpieza", 40);

        assertThatThrownBy(() -> prestacion.cambiarDuracion(duracion))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("duración");
        assertThat(prestacion.getDuracionMinutos()).isEqualTo(40);
    }

    @Test
    void desactivarNoTocaNombreNiDuracionYEsIdempotente() {
        Prestacion prestacion = Prestacion.nueva("Limpieza", 40);

        prestacion.desactivar();
        prestacion.desactivar();

        assertThat(prestacion.isActiva()).isFalse();
        assertThat(prestacion.getNombre()).isEqualTo("Limpieza");
        assertThat(prestacion.getDuracionMinutos()).isEqualTo(40);
    }

    @Test
    void activarLaReactivaYEsIdempotente() {
        Prestacion prestacion = Prestacion.nueva("Limpieza", 40);
        prestacion.desactivar();

        prestacion.activar();
        prestacion.activar();

        assertThat(prestacion.isActiva()).isTrue();
    }

    @Test
    void igualdadPorId() {
        Prestacion a = AyudanteDeIds.conId(Prestacion.nueva("Limpieza", 40), 7L);
        Prestacion mismoId = AyudanteDeIds.conId(Prestacion.nueva("Otra", 10), 7L);
        Prestacion otroId = AyudanteDeIds.conId(Prestacion.nueva("Limpieza", 40), 8L);

        assertThat(a).isEqualTo(mismoId).hasSameHashCodeAs(mismoId).isNotEqualTo(otroId);
    }

    @Test
    void sinIdSoloEsIgualASiMisma() {
        Prestacion a = Prestacion.nueva("Limpieza", 40);
        Prestacion b = Prestacion.nueva("Limpieza", 40);

        assertThat(a).isEqualTo(a).isNotEqualTo(b);
    }

    // --- C-05 gestion-recursos — "Edición de recursos"

    @Test
    void renombrarCambiaElNombreRecortado() {
        Prestacion limpieza = Prestacion.nueva("Limpieza", 40);

        limpieza.renombrar("  Limpieza profunda ");

        assertThat(limpieza.getNombre()).isEqualTo("Limpieza profunda");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void renombrarEnBlancoSeRechazaYNoCambia(String vacio) {
        Prestacion limpieza = Prestacion.nueva("Limpieza", 40);

        assertThatThrownBy(() -> limpieza.renombrar(vacio))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("nombre");
        assertThat(limpieza.getNombre()).isEqualTo("Limpieza");
    }

    @Test
    void actualizarCambiaNombreYDuracionSinTocarElEstado() {
        Prestacion limpieza = Prestacion.nueva("Limpieza", 40);
        limpieza.desactivar();

        limpieza.actualizar("Limpieza simple", 45);

        assertThat(limpieza.getNombre()).isEqualTo("Limpieza simple");
        assertThat(limpieza.getDuracionMinutos()).isEqualTo(45);
        assertThat(limpieza.isActiva()).isFalse();
    }

    @Test
    void actualizarEsTodoONada() {
        Prestacion limpieza = Prestacion.nueva("Limpieza", 40);

        assertThatThrownBy(() -> limpieza.actualizar("Otro nombre", 0))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("duración");
        assertThatThrownBy(() -> limpieza.actualizar(" ", 50))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("nombre");

        assertThat(limpieza.getNombre()).isEqualTo("Limpieza");
        assertThat(limpieza.getDuracionMinutos()).isEqualTo(40);
    }
}

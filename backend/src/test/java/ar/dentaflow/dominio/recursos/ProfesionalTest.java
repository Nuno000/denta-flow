package ar.dentaflow.dominio.recursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Spec recursos-consultorio — "Datos obligatorios", "Desactivar sin borrar" y
 * "Prestaciones que realiza cada profesional".
 */
class ProfesionalTest {

    private static Profesional laura() {
        return Profesional.nuevo("Laura", "Ejemplo", "MP-0001", "laura.ejemplo@example.com");
    }

    private static Prestacion prestacionActiva(long id) {
        return AyudanteDeIds.conId(Prestacion.nueva("Prestación " + id, 30), id);
    }

    @Test
    void altaConTodosLosDatosQuedaActivaYSinPrestaciones() {
        Profesional laura = laura();

        assertThat(laura.getNombre()).isEqualTo("Laura");
        assertThat(laura.getApellido()).isEqualTo("Ejemplo");
        assertThat(laura.getMatricula()).isEqualTo("MP-0001");
        assertThat(laura.getEmail()).isEqualTo("laura.ejemplo@example.com");
        assertThat(laura.isActivo()).isTrue();
        assertThat(laura.prestacionIds()).isEmpty();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "  "})
    void matriculaAusenteOEnBlancoQuedaNula(String matricula) {
        Profesional profesional = Profesional.nuevo("Martín", "Prueba", matricula, "martin@example.com");

        assertThat(profesional.getMatricula()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rechazaNombreApellidoYEmailEnBlanco(String vacio) {
        assertThatThrownBy(() -> Profesional.nuevo(vacio, "Ejemplo", null, "a@b.co"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("nombre");
        assertThatThrownBy(() -> Profesional.nuevo("Laura", vacio, null, "a@b.co"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("apellido");
        assertThatThrownBy(() -> Profesional.nuevo("Laura", "Ejemplo", null, vacio))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba", "@sinusuario.com", "usuario@", "usuario@dominio", "dos@@dominio.com", "con espacio@dominio.com"})
    void rechazaEmailSinFormatoMinimo(String email) {
        assertThatThrownBy(() -> Profesional.nuevo("Laura", "Ejemplo", null, email))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("email");
    }

    @Test
    void guardaElEmailEnMinusculasYRecortado() {
        Profesional profesional = Profesional.nuevo("Laura", "Ejemplo", null, "  Laura.Ejemplo@Example.COM ");

        assertThat(profesional.getEmail()).isEqualTo("laura.ejemplo@example.com");
    }

    @Test
    void respetaLosLargosMaximos() {
        assertThat(Profesional.nuevo("n".repeat(100), "a".repeat(100), "m".repeat(30),
                "u".repeat(247) + "@ej.com").getEmail()).hasSize(254);

        assertThatThrownBy(() -> Profesional.nuevo("n".repeat(101), "a", null, "a@b.co"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("100");
        assertThatThrownBy(() -> Profesional.nuevo("n", "a".repeat(101), null, "a@b.co"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("100");
        assertThatThrownBy(() -> Profesional.nuevo("n", "a", "m".repeat(31), "a@b.co"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("30");
        assertThatThrownBy(() -> Profesional.nuevo("n", "a", null, "u".repeat(248) + "@ej.com"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("254");
    }

    @Test
    void habilitarPrestacionActivaAgregaSuId() {
        Profesional laura = laura();

        laura.habilitarPrestacion(prestacionActiva(5));
        laura.habilitarPrestacion(prestacionActiva(9));

        assertThat(laura.prestacionIds()).containsExactlyInAnyOrder(5L, 9L);
        assertThat(laura.realiza(5L)).isTrue();
        assertThat(laura.realiza(7L)).isFalse();
    }

    @Test
    void habilitarDosVecesLaMismaPrestacionNoDuplica() {
        Profesional laura = laura();

        laura.habilitarPrestacion(prestacionActiva(5));
        laura.habilitarPrestacion(prestacionActiva(5));

        assertThat(laura.prestacionIds()).containsExactly(5L);
    }

    @Test
    void rechazaPrestacionInactivaSinCambiarAlProfesional() {
        Profesional laura = laura();
        Prestacion inactiva = prestacionActiva(5);
        inactiva.desactivar();

        assertThatThrownBy(() -> laura.habilitarPrestacion(inactiva))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("inactiva");
        assertThat(laura.prestacionIds()).isEmpty();
    }

    @Test
    void rechazaPrestacionSinIdOnula() {
        Profesional laura = laura();

        assertThatThrownBy(() -> laura.habilitarPrestacion(Prestacion.nueva("Sin guardar", 30)))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> laura.habilitarPrestacion(null))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(laura.prestacionIds()).isEmpty();
    }

    @Test
    void deshabilitarQuitaSoloEsaPrestacionYEsTolerante() {
        Profesional laura = laura();
        laura.habilitarPrestacion(prestacionActiva(5));
        laura.habilitarPrestacion(prestacionActiva(9));

        laura.deshabilitarPrestacion(5L);
        laura.deshabilitarPrestacion(5L);

        assertThat(laura.prestacionIds()).containsExactly(9L);
    }

    @Test
    void prestacionIdsNoSePuedeModificarDesdeAfuera() {
        Profesional laura = laura();
        laura.habilitarPrestacion(prestacionActiva(5));

        assertThatThrownBy(() -> laura.prestacionIds().add(99L))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(laura.prestacionIds()).containsExactly(5L);
    }

    @Test
    void desactivarYReactivarConservaLasPrestaciones() {
        Profesional laura = laura();
        laura.habilitarPrestacion(prestacionActiva(5));

        laura.desactivar();
        laura.desactivar();
        assertThat(laura.isActivo()).isFalse();
        assertThat(laura.prestacionIds()).containsExactly(5L);

        laura.activar();
        assertThat(laura.isActivo()).isTrue();
        assertThat(laura.prestacionIds()).containsExactly(5L);
    }

    @Test
    void igualdadPorIdYSinIdSoloIgualASiMismo() {
        Profesional a = AyudanteDeIds.conId(laura(), 3L);
        Profesional mismoId = AyudanteDeIds.conId(
                Profesional.nuevo("Otro", "Otro", null, "otro@example.com"), 3L);
        Profesional otroId = AyudanteDeIds.conId(laura(), 4L);
        Profesional sinId = laura();

        assertThat(a).isEqualTo(mismoId).hasSameHashCodeAs(mismoId).isNotEqualTo(otroId);
        assertThat(sinId).isEqualTo(sinId).isNotEqualTo(laura());
    }

    // --- C-05 gestion-recursos — "Edición de recursos"

    @Test
    void actualizarDatosCambiaTodosLosDatosYPasaElEmailAMinusculas() {
        Profesional laura = laura();

        laura.actualizarDatos(" Laura Inés ", "Ejemplo Nuevo", "MP-0002", "Laura.Nueva@Example.com");

        assertThat(laura.getNombre()).isEqualTo("Laura Inés");
        assertThat(laura.getApellido()).isEqualTo("Ejemplo Nuevo");
        assertThat(laura.getMatricula()).isEqualTo("MP-0002");
        assertThat(laura.getEmail()).isEqualTo("laura.nueva@example.com");
    }

    @Test
    void actualizarDatosConMatriculaVaciaLaDejaNulaYConservaEstadoYPrestaciones() {
        Profesional laura = laura();
        laura.habilitarPrestacion(prestacionActiva(5L));
        laura.desactivar();

        laura.actualizarDatos("Laura", "Ejemplo", "  ", "laura.ejemplo@example.com");

        assertThat(laura.getMatricula()).isNull();
        assertThat(laura.isActivo()).isFalse();
        assertThat(laura.prestacionIds()).containsExactly(5L);
    }

    @Test
    void actualizarDatosInvalidosNoCambiaNingunCampo() {
        Profesional laura = laura();
        String nombreLargo = "N".repeat(Profesional.NOMBRE_MAXIMO + 1);

        assertThatThrownBy(() -> laura.actualizarDatos("Otra", " ", "MP-9", "otra@example.com"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("apellido");
        assertThatThrownBy(() -> laura.actualizarDatos("Otra", "Persona", "MP-9", "mal-formado"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("email");
        assertThatThrownBy(() -> laura.actualizarDatos(nombreLargo, "Persona", "MP-9", "otra@example.com"))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("nombre");

        assertThat(laura.getNombre()).isEqualTo("Laura");
        assertThat(laura.getApellido()).isEqualTo("Ejemplo");
        assertThat(laura.getMatricula()).isEqualTo("MP-0001");
        assertThat(laura.getEmail()).isEqualTo("laura.ejemplo@example.com");
    }
}

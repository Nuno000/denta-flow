package ar.dentaflow.dominio.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec parametros-consultorio — anticipación en horas enteras, textos del aviso y confirmación al guardar. */
class ParametroTest {

    private static final Instant AHORA = Instant.parse("2026-12-02T00:30:00Z");

    /** Las filas las crea la migración: el test construye una provisoria por reflexión, sin fábrica pública. */
    static Parametro provisorio(ClaveParametro clave, String valor) {
        try {
            Constructor<Parametro> constructor = Parametro.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Parametro parametro = constructor.newInstance();
            asignar(parametro, "clave", clave.codigo());
            asignar(parametro, "valor", valor);
            asignar(parametro, "provisorio", true);
            return parametro;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void asignar(Parametro parametro, String campo, Object valor) throws ReflectiveOperationException {
        Field f = Parametro.class.getDeclaredField(campo);
        f.setAccessible(true);
        f.set(parametro, valor);
    }

    private static Parametro anticipacion() {
        return provisorio(ClaveParametro.ANTICIPACION_MINIMA_HORAS, "24");
    }

    @Test
    void actualizarLaAnticipacionGuardaValorAutorEInstanteYQuitaLaMarcaProvisoria() {
        Parametro parametro = anticipacion();

        parametro.actualizar("48", 7L, AHORA);

        assertThat(parametro.valor()).isEqualTo("48");
        assertThat(parametro.valorEntero()).isEqualTo(48);
        assertThat(parametro.isProvisorio()).isFalse();
        assertThat(parametro.actualizadoPor()).isEqualTo(7L);
        assertThat(parametro.actualizadoEn()).isEqualTo(AHORA);
        assertThat(parametro.clave()).isEqualTo(ClaveParametro.ANTICIPACION_MINIMA_HORAS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "168"})
    void aceptaLosExtremosDelRango(String valor) {
        Parametro parametro = anticipacion();

        parametro.actualizar(valor, 7L, AHORA);

        assertThat(parametro.valor()).isEqualTo(valor);
    }

    @Test
    void recortaLosEspaciosDelEntero() {
        Parametro parametro = anticipacion();

        parametro.actualizar(" 36 ", 7L, AHORA);

        assertThat(parametro.valorEntero()).isEqualTo(36);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-1", "-5", "169", "veinte", "24.5", "+48", "   ", "1234567890"})
    void rechazaUnaAnticipacionInvalidaYNoCambiaNada(String invalido) {
        Parametro parametro = anticipacion();

        assertThatThrownBy(() -> parametro.actualizar(invalido, 7L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessage("La anticipación mínima debe ser un número entero de horas entre 0 y 168");

        assertThat(parametro.valor()).isEqualTo("24");
        assertThat(parametro.isProvisorio()).isTrue();
        assertThat(parametro.actualizadoPor()).isNull();
        assertThat(parametro.actualizadoEn()).isNull();
    }

    @Test
    void elTextoDelAvisoSeGuardaRecortado() {
        Parametro parametro = provisorio(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "borrador");
        String texto = "a".repeat(1500);

        parametro.actualizar("  " + texto + "  ", 7L, AHORA);

        assertThat(parametro.valor()).isEqualTo(texto);
        assertThat(parametro.isProvisorio()).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rechazaUnaLeyendaVaciaOEnBlanco(String vacio) {
        Parametro parametro = provisorio(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA, "Acepto");

        assertThatThrownBy(() -> parametro.actualizar(vacio, 7L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThat(parametro.valor()).isEqualTo("Acepto");
        assertThat(parametro.isProvisorio()).isTrue();
    }

    @Test
    void respetaLosMaximosDeTextoYLeyendaYNombraElLimiteEnElError() {
        Parametro texto = provisorio(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "borrador");
        Parametro leyenda = provisorio(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA, "Acepto");

        texto.actualizar("t".repeat(4000), 7L, AHORA);
        leyenda.actualizar("l".repeat(200), 7L, AHORA);
        assertThat(texto.valor()).hasSize(4000);
        assertThat(leyenda.valor()).hasSize(200);

        assertThatThrownBy(() -> texto.actualizar("t".repeat(4001), 7L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("4000");
        assertThatThrownBy(() -> leyenda.actualizar("l".repeat(201), 7L, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("200");
        assertThat(texto.valor()).hasSize(4000);
    }

    @Test
    void guardarElMismoValorTambienConfirma() {
        Parametro parametro = anticipacion();

        parametro.actualizar("24", 7L, AHORA);

        assertThat(parametro.valor()).isEqualTo("24");
        assertThat(parametro.isProvisorio()).isFalse();
        assertThat(parametro.actualizadoPor()).isEqualTo(7L);
    }

    @Test
    void valorEnteroSobreUnaClaveDeTextoEsUnErrorDeProgramacion() {
        Parametro parametro = provisorio(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "borrador");

        assertThatThrownBy(parametro::valorEntero).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void dosParametrosConLaMismaClaveSonIguales() {
        Parametro uno = anticipacion();
        Parametro otro = anticipacion();
        otro.actualizar("48", 7L, AHORA);

        assertThat(uno).isEqualTo(otro).hasSameHashCodeAs(otro);
        assertThat(uno).isNotEqualTo(provisorio(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "x"));
    }
}

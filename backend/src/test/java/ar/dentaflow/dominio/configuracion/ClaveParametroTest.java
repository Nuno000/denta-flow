package ar.dentaflow.dominio.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec parametros-consultorio — "Catálogo cerrado de parámetros". */
class ClaveParametroTest {

    @ParameterizedTest
    @CsvSource({
            "anticipacion-minima-horas, ANTICIPACION_MINIMA_HORAS",
            "aviso-privacidad-texto, AVISO_PRIVACIDAD_TEXTO",
            "aviso-privacidad-leyenda, AVISO_PRIVACIDAD_LEYENDA",
            "contacto-consultorio, CONTACTO_CONSULTORIO"})
    void desdeCodigoDevuelveLaClaveDelCatalogo(String codigo, ClaveParametro esperada) {
        assertThat(ClaveParametro.desdeCodigo(codigo)).isEqualTo(esperada);
        assertThat(esperada.codigo()).isEqualTo(codigo);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"recordatorio-horas", "ANTICIPACION-MINIMA-HORAS"})
    void unCodigoFueraDelCatalogoEsUnRecursoNoEncontrado(String codigo) {
        assertThatThrownBy(() -> ClaveParametro.desdeCodigo(codigo))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("No existe el parámetro " + codigo);
    }

    @Test
    void laAnticipacionEsUnEnteroDeHorasEntreCeroYCientoSesentaYOcho() {
        ClaveParametro clave = ClaveParametro.ANTICIPACION_MINIMA_HORAS;

        assertThat(clave.tipo()).isEqualTo(TipoParametro.ENTERO);
        assertThat(clave.unidad()).isEqualTo("horas");
        assertThat(clave.minimo()).isEqualTo(0);
        assertThat(clave.maximo()).isEqualTo(168);
        assertThat(clave.longitudMaxima()).isNull();
        assertThat(clave.descripcion()).isNotBlank();
    }

    @Test
    void losTextosDelAvisoTienenLongitudMaximaYNoUnidad() {
        assertThat(ClaveParametro.AVISO_PRIVACIDAD_TEXTO.tipo()).isEqualTo(TipoParametro.TEXTO);
        assertThat(ClaveParametro.AVISO_PRIVACIDAD_TEXTO.longitudMaxima()).isEqualTo(4000);
        assertThat(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA.longitudMaxima()).isEqualTo(200);
        assertThat(ClaveParametro.CONTACTO_CONSULTORIO.tipo()).isEqualTo(TipoParametro.TEXTO);
        assertThat(ClaveParametro.CONTACTO_CONSULTORIO.longitudMaxima()).isEqualTo(300);
        for (ClaveParametro clave : new ClaveParametro[] {
                ClaveParametro.AVISO_PRIVACIDAD_TEXTO, ClaveParametro.AVISO_PRIVACIDAD_LEYENDA,
                ClaveParametro.CONTACTO_CONSULTORIO}) {
            assertThat(clave.unidad()).isNull();
            assertThat(clave.minimo()).isNull();
            assertThat(clave.maximo()).isNull();
            assertThat(clave.descripcion()).isNotBlank();
        }
    }
}

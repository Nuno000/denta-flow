package ar.dentaflow.aplicacion.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.configuracion.ClaveParametro;
import ar.dentaflow.dominio.configuracion.Parametro;
import ar.dentaflow.dominio.configuracion.ParametroRepositorio;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Spec parametros-consultorio — edición, listado en orden de catálogo y lectura tipada. */
@ExtendWith(MockitoExtension.class)
class ServicioDeParametrosTest {

    private static final Instant AHORA = Instant.parse("2026-12-02T00:30:00Z");

    @Mock
    private ParametroRepositorio repositorio;

    private ServicioDeParametros servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioDeParametros(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Parametro fila(ClaveParametro clave, String valor) {
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

    @Test
    void actualizarGuardaElValorConElAutorYElInstanteDelReloj() {
        Parametro existente = fila(ClaveParametro.ANTICIPACION_MINIMA_HORAS, "24");
        when(repositorio.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS)).thenReturn(Optional.of(existente));
        when(repositorio.guardar(any())).thenAnswer(i -> i.getArgument(0));

        Parametro resultado = servicio.actualizar("anticipacion-minima-horas", "48", 7L);

        ArgumentCaptor<Parametro> guardado = ArgumentCaptor.forClass(Parametro.class);
        verify(repositorio).guardar(guardado.capture());
        assertThat(guardado.getValue().valorEntero()).isEqualTo(48);
        assertThat(guardado.getValue().actualizadoPor()).isEqualTo(7L);
        assertThat(guardado.getValue().actualizadoEn()).isEqualTo(AHORA);
        assertThat(guardado.getValue().isProvisorio()).isFalse();
        assertThat(resultado).isSameAs(existente);
    }

    @Test
    void unaClaveDesconocidaEsNoEncontradaSinTocarElRepositorio() {
        assertThatThrownBy(() -> servicio.actualizar("recordatorio-horas", "48", 7L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.obtener("recordatorio-horas"))
                .isInstanceOf(RecursoNoEncontradoException.class);

        verifyNoInteractions(repositorio);
    }

    @Test
    void unValorInvalidoPropagaElErrorYNoGuarda() {
        when(repositorio.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS))
                .thenReturn(Optional.of(fila(ClaveParametro.ANTICIPACION_MINIMA_HORAS, "24")));

        assertThatThrownBy(() -> servicio.actualizar("anticipacion-minima-horas", "169", 7L))
                .isInstanceOf(ValidacionDeDominioException.class);

        verify(repositorio, never()).guardar(any());
    }

    @Test
    void listarDevuelveLasFilasEnElOrdenDelCatalogoAunqueElRepositorioLasDevuelvaEnOtro() {
        when(repositorio.listarTodos()).thenReturn(List.of(
                fila(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA, "l"),
                fila(ClaveParametro.ANTICIPACION_MINIMA_HORAS, "24"),
                fila(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "t")));

        assertThat(servicio.listar()).extracting(Parametro::clave).containsExactly(
                ClaveParametro.ANTICIPACION_MINIMA_HORAS, ClaveParametro.AVISO_PRIVACIDAD_TEXTO,
                ClaveParametro.AVISO_PRIVACIDAD_LEYENDA);
    }

    @Test
    void obtenerDevuelveLaFilaDeLaClave() {
        Parametro texto = fila(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "t");
        when(repositorio.buscar(ClaveParametro.AVISO_PRIVACIDAD_TEXTO)).thenReturn(Optional.of(texto));

        assertThat(servicio.obtener("aviso-privacidad-texto")).isSameAs(texto);
    }

    @Test
    void laAnticipacionMinimaEsUnaDuracionEnHoras() {
        when(repositorio.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS))
                .thenReturn(Optional.of(fila(ClaveParametro.ANTICIPACION_MINIMA_HORAS, "36")));

        assertThat(servicio.anticipacionMinima()).isEqualTo(Duration.ofHours(36));
    }

    @Test
    void elAvisoDePrivacidadDevuelveTextoYLeyendaAunqueSeanProvisorios() {
        when(repositorio.buscar(ClaveParametro.AVISO_PRIVACIDAD_TEXTO))
                .thenReturn(Optional.of(fila(ClaveParametro.AVISO_PRIVACIDAD_TEXTO, "el texto")));
        when(repositorio.buscar(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA))
                .thenReturn(Optional.of(fila(ClaveParametro.AVISO_PRIVACIDAD_LEYENDA, "la leyenda")));

        AvisoPrivacidad aviso = servicio.avisoPrivacidad();

        assertThat(aviso.texto()).isEqualTo("el texto");
        assertThat(aviso.leyendaAceptacion()).isEqualTo("la leyenda");
    }

    @Test
    void siFaltaLaFilaDeUnaClaveDelCatalogoEsUnErrorQueLaNombra() {
        when(repositorio.buscar(ClaveParametro.ANTICIPACION_MINIMA_HORAS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.anticipacionMinima())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("anticipacion-minima-horas");
        assertThatThrownBy(() -> servicio.obtener("anticipacion-minima-horas"))
                .isInstanceOf(IllegalStateException.class);
    }
}

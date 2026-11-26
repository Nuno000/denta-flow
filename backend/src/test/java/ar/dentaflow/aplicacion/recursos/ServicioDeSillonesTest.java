package ar.dentaflow.aplicacion.recursos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec gestion-recursos — alta, edición, duplicados y desactivación de sillones (design D1, D4). */
class ServicioDeSillonesTest {

    private final SillonRepositorio sillones = mock(SillonRepositorio.class);
    private final ServicioDeSillones servicio = new ServicioDeSillones(sillones);

    private Sillon sillon1;
    private Sillon sillon2;

    @BeforeEach
    void preparar() {
        sillon1 = AyudanteDeIds.conId(Sillon.nuevo("Sillón 1"), 1L);
        sillon2 = AyudanteDeIds.conId(Sillon.nuevo("Sillón 2"), 2L);
        when(sillones.listarTodos()).thenReturn(List.of(sillon1, sillon2));
        when(sillones.buscarPorId(1L)).thenReturn(Optional.of(sillon1));
        when(sillones.buscarPorId(2L)).thenReturn(Optional.of(sillon2));
        when(sillones.buscarPorId(99L)).thenReturn(Optional.empty());
        when(sillones.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crearGuardaUnSillonActivo() {
        Sillon creado = servicio.crear(" Sillón 3 ");

        assertThat(creado.getNombre()).isEqualTo("Sillón 3");
        assertThat(creado.isActivo()).isTrue();
        verify(sillones).guardar(creado);
    }

    @Test
    void crearConNombreRepetidoIgnorandoMayusculasYEspaciosEsConflicto() {
        assertThatThrownBy(() -> servicio.crear("sillón 1 "))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un sillón con ese nombre");
        verify(sillones, never()).guardar(any());
    }

    @Test
    void crearConNombreEnBlancoEsInvalido() {
        assertThatThrownBy(() -> servicio.crear("  ")).isInstanceOf(ValidacionDeDominioException.class);
        verify(sillones, never()).guardar(any());
    }

    @Test
    void renombrarExcluyeAlPropioSillon() {
        Sillon renombrado = servicio.renombrar(1L, "SILLÓN 1");

        assertThat(renombrado.getNombre()).isEqualTo("SILLÓN 1");
    }

    @Test
    void renombrarConElNombreDeOtroEsConflictoYNoCambia() {
        assertThatThrownBy(() -> servicio.renombrar(1L, "Sillón 2")).isInstanceOf(ConflictoException.class);

        assertThat(sillon1.getNombre()).isEqualTo("Sillón 1");
        verify(sillones, never()).guardar(any());
    }

    @Test
    void sillonInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.renombrar(99L, "X")).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.desactivar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.activar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivarYActivarSonIdempotentes() {
        servicio.desactivar(2L);
        assertThat(servicio.desactivar(2L).isActivo()).isFalse();

        servicio.activar(2L);
        assertThat(servicio.activar(2L).isActivo()).isTrue();
    }

    @Test
    void listarDevuelveActivosEInactivos() {
        sillon2.desactivar();

        assertThat(servicio.listar()).containsExactly(sillon1, sillon2);
    }
}

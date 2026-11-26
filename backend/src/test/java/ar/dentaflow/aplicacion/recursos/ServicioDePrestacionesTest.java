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
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec gestion-recursos — alta, edición, duplicados y desactivación de prestaciones (design D1, D4). */
class ServicioDePrestacionesTest {

    private final PrestacionRepositorio prestaciones = mock(PrestacionRepositorio.class);
    private final ServicioDePrestaciones servicio = new ServicioDePrestaciones(prestaciones);

    private Prestacion limpieza;
    private Prestacion consulta;

    @BeforeEach
    void preparar() {
        consulta = AyudanteDeIds.conId(Prestacion.nueva("Consulta", 30), 1L);
        limpieza = AyudanteDeIds.conId(Prestacion.nueva("Limpieza", 40), 2L);
        when(prestaciones.listarTodas()).thenReturn(List.of(consulta, limpieza));
        when(prestaciones.buscarPorId(1L)).thenReturn(Optional.of(consulta));
        when(prestaciones.buscarPorId(2L)).thenReturn(Optional.of(limpieza));
        when(prestaciones.buscarPorId(99L)).thenReturn(Optional.empty());
        when(prestaciones.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void crearGuardaUnaPrestacionActivaConSuDuracion() {
        Prestacion creada = servicio.crear("Extracción", 60);

        assertThat(creada.getNombre()).isEqualTo("Extracción");
        assertThat(creada.getDuracionMinutos()).isEqualTo(60);
        assertThat(creada.isActiva()).isTrue();
        verify(prestaciones).guardar(creada);
    }

    @Test
    void crearConNombreRepetidoEsConflicto() {
        assertThatThrownBy(() -> servicio.crear(" LIMPIEZA", 20))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe una prestación con ese nombre");
        verify(prestaciones, never()).guardar(any());
    }

    @Test
    void crearConDuracionInvalidaEsInvalido() {
        assertThatThrownBy(() -> servicio.crear("Control", 0)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.crear("Control", null)).isInstanceOf(ValidacionDeDominioException.class);
        verify(prestaciones, never()).guardar(any());
    }

    @Test
    void actualizarSoloLaDuracionConservandoElPropioNombreSeAcepta() {
        Prestacion editada = servicio.actualizar(2L, "Limpieza", 45);

        assertThat(editada.getNombre()).isEqualTo("Limpieza");
        assertThat(editada.getDuracionMinutos()).isEqualTo(45);
        assertThat(editada.isActiva()).isTrue();
    }

    @Test
    void actualizarConElNombreDeOtraEsConflictoYNoCambia() {
        assertThatThrownBy(() -> servicio.actualizar(2L, "consulta", 45)).isInstanceOf(ConflictoException.class);

        assertThat(limpieza.getNombre()).isEqualTo("Limpieza");
        assertThat(limpieza.getDuracionMinutos()).isEqualTo(40);
    }

    @Test
    void actualizarConDuracionCeroNoCambiaNada() {
        assertThatThrownBy(() -> servicio.actualizar(2L, "Limpieza", 0))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThat(limpieza.getDuracionMinutos()).isEqualTo(40);
    }

    @Test
    void prestacionInexistenteEsNoEncontrada() {
        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.actualizar(99L, "X", 10)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.desactivar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.activar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivarYActivarSonIdempotentes() {
        servicio.desactivar(2L);
        assertThat(servicio.desactivar(2L).isActiva()).isFalse();

        servicio.activar(2L);
        assertThat(servicio.activar(2L).isActiva()).isTrue();
    }

    @Test
    void listarDevuelveActivasEInactivas() {
        consulta.desactivar();

        assertThat(servicio.listar()).containsExactly(consulta, limpieza);
    }
}

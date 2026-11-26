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
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spec gestion-recursos — alta, edición, duplicados y desactivación de profesionales, y asignación de
 * prestaciones (design D1, D4, D5).
 */
class ServicioDeProfesionalesTest {

    private final ProfesionalRepositorio profesionales = mock(ProfesionalRepositorio.class);
    private final PrestacionRepositorio prestaciones = mock(PrestacionRepositorio.class);
    private final ServicioDeProfesionales servicio = new ServicioDeProfesionales(profesionales, prestaciones);

    private Profesional laura;
    private Profesional martin;
    private Prestacion consulta;
    private Prestacion limpieza;
    private Prestacion extraccion;
    private Prestacion blanqueamiento;

    @BeforeEach
    void preparar() {
        consulta = AyudanteDeIds.conId(Prestacion.nueva("Consulta", 30), 1L);
        limpieza = AyudanteDeIds.conId(Prestacion.nueva("Limpieza", 40), 2L);
        extraccion = AyudanteDeIds.conId(Prestacion.nueva("Extracción", 60), 3L);
        blanqueamiento = AyudanteDeIds.conId(Prestacion.nueva("Blanqueamiento", 90), 4L);
        blanqueamiento.desactivar();
        for (Prestacion p : List.of(consulta, limpieza, extraccion, blanqueamiento)) {
            when(prestaciones.buscarPorId(p.getId())).thenReturn(Optional.of(p));
        }
        when(prestaciones.buscarPorId(99L)).thenReturn(Optional.empty());

        laura = AyudanteDeIds.conId(Profesional.nuevo("Laura", "Ejemplo", "MP-0001", "laura@example.com"), 10L);
        laura.habilitarPrestacion(limpieza);
        martin = AyudanteDeIds.conId(Profesional.nuevo("Martín", "Prueba", null, "martin@example.com"), 11L);
        when(profesionales.listarTodos()).thenReturn(List.of(laura, martin));
        when(profesionales.buscarPorId(10L)).thenReturn(Optional.of(laura));
        when(profesionales.buscarPorId(11L)).thenReturn(Optional.of(martin));
        when(profesionales.buscarPorId(99L)).thenReturn(Optional.empty());
        when(profesionales.guardar(any())).thenAnswer(i -> i.getArgument(0));
    }

    // --- Alta y edición

    @Test
    void crearGuardaUnProfesionalActivoSinPrestaciones() {
        Profesional creado = servicio.crear("Ana", "Muestra", null, "Ana@Example.com");

        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.getMatricula()).isNull();
        assertThat(creado.getEmail()).isEqualTo("ana@example.com");
        assertThat(creado.prestacionIds()).isEmpty();
        verify(profesionales).guardar(creado);
    }

    @Test
    void crearConEmailDeOtroEsConflicto() {
        assertThatThrownBy(() -> servicio.crear("Ana", "Muestra", null, "LAURA@example.com"))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un profesional con ese email");
        verify(profesionales, never()).guardar(any());
    }

    @Test
    void crearConMatriculaDeOtroEsConflicto() {
        assertThatThrownBy(() -> servicio.crear("Ana", "Muestra", "mp-0001", "ana@example.com"))
                .isInstanceOf(ConflictoException.class)
                .hasMessage("Ya existe un profesional con esa matrícula");
    }

    @Test
    void dosProfesionalesSinMatriculaSeAceptan() {
        Profesional creado = servicio.crear("Ana", "Muestra", "", "ana@example.com");

        assertThat(creado.getMatricula()).isNull();
    }

    @Test
    void crearConDatosInvalidosEsInvalido() {
        assertThatThrownBy(() -> servicio.crear("Ana", " ", null, "ana@example.com"))
                .isInstanceOf(ValidacionDeDominioException.class);
        verify(profesionales, never()).guardar(any());
    }

    @Test
    void editarConservandoElPropioEmailSeAcepta() {
        Profesional editado = servicio.actualizar(10L, "Laura Inés", "Ejemplo", "MP-0001", "laura@example.com");

        assertThat(editado.getNombre()).isEqualTo("Laura Inés");
        assertThat(editado.prestacionIds()).containsExactly(2L);
    }

    @Test
    void editarConElEmailDeOtroEsConflictoYNoCambia() {
        assertThatThrownBy(() -> servicio.actualizar(11L, "Martín", "Prueba", null, "laura@example.com"))
                .isInstanceOf(ConflictoException.class);

        assertThat(martin.getEmail()).isEqualTo("martin@example.com");
        assertThat(laura.getEmail()).isEqualTo("laura@example.com");
    }

    @Test
    void profesionalInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> servicio.obtener(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.actualizar(99L, "A", "B", null, "a@example.com"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.desactivar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.activar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.asignarPrestaciones(99L, List.of(1L)))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivarConservaPrestacionesYEsIdempotente() {
        servicio.desactivar(10L);
        Profesional desactivado = servicio.desactivar(10L);

        assertThat(desactivado.isActivo()).isFalse();
        assertThat(desactivado.prestacionIds()).containsExactly(2L);
        assertThat(servicio.activar(10L).isActivo()).isTrue();
    }

    // --- Asignación de prestaciones (design D5)

    @Test
    void asignarReemplazaElConjunto() {
        Profesional resultado = servicio.asignarPrestaciones(10L, List.of(1L, 3L));

        assertThat(resultado.prestacionIds()).containsExactlyInAnyOrder(1L, 3L);
        verify(profesionales).guardar(laura);
    }

    @Test
    void agregarUnaInactivaSeRechazaSinCambiarNada() {
        assertThatThrownBy(() -> servicio.asignarPrestaciones(10L, List.of(1L, 4L)))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("inactiva");

        assertThat(laura.prestacionIds()).containsExactly(2L);
    }

    @Test
    void conservarUnaInactivaYaAsociadaSeAcepta() {
        // Martín tenía Blanqueamiento (id 4) cuando estaba activa; después se desactivó.
        martin.habilitarPrestacion(activaTemporal(4L));

        Profesional resultado = servicio.asignarPrestaciones(11L, List.of(4L, 1L));

        assertThat(resultado.prestacionIds()).containsExactlyInAnyOrder(1L, 4L);
    }

    @Test
    void prestacionInexistenteSeRechazaNombrandoElIdSinCambiarNada() {
        assertThatThrownBy(() -> servicio.asignarPrestaciones(10L, List.of(1L, 99L)))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("99");

        assertThat(laura.prestacionIds()).containsExactly(2L);
    }

    @Test
    void conjuntoVacioDejaAlProfesionalSinPrestaciones() {
        assertThat(servicio.asignarPrestaciones(10L, List.of()).prestacionIds()).isEmpty();
    }

    @Test
    void idsRepetidosSeAsocianUnaSolaVez() {
        assertThat(servicio.asignarPrestaciones(10L, List.of(1L, 1L, 2L)).prestacionIds())
                .containsExactlyInAnyOrder(1L, 2L);
    }

    /** Una prestación activa con el id dado, para simular que el profesional la tenía antes de desactivarse. */
    private static Prestacion activaTemporal(long id) {
        return AyudanteDeIds.conId(Prestacion.nueva("Temporal " + id, 30), id);
    }
}

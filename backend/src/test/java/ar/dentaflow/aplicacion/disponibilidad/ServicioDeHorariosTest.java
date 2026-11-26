package ar.dentaflow.aplicacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Spec horarios-atencion — alta, edición, baja y consulta. El servicio bloquea la fila del
 * profesional antes de leer los rangos del día y guardar (design D6).
 */
@ExtendWith(MockitoExtension.class)
class ServicioDeHorariosTest {

    private static final Long PROFESIONAL = 3L;
    private static final LocalTime NUEVE = LocalTime.of(9, 0);
    private static final LocalTime TRECE = LocalTime.of(13, 0);

    @Mock
    private HorarioAtencionRepositorio horarios;

    @Mock
    private ProfesionalRepositorio profesionales;

    private ServicioDeHorarios servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioDeHorarios(horarios, profesionales);
    }

    private static Profesional profesional(boolean activo) {
        Profesional profesional = AyudanteDeIds.conId(
                Profesional.nuevo("Laura", "Ejemplo", null, "laura@example.com"), PROFESIONAL);
        if (!activo) {
            profesional.desactivar();
        }
        return profesional;
    }

    private static HorarioAtencion rango(long id, DayOfWeek dia, String inicio, String fin) {
        return AyudanteDeIds.conId(
                HorarioAtencion.nuevo(PROFESIONAL, dia, LocalTime.parse(inicio), LocalTime.parse(fin)), id);
    }

    @Test
    void crearBloqueaAlProfesionalAntesDeLeerYGuardar() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY)).thenReturn(List.of());
        when(horarios.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

        HorarioAtencion creado = servicio.crear(PROFESIONAL, DayOfWeek.MONDAY, NUEVE, TRECE);

        assertThat(creado.getInicio()).isEqualTo(NUEVE);
        InOrder orden = inOrder(profesionales, horarios);
        orden.verify(profesionales).bloquearPorId(PROFESIONAL);
        orden.verify(horarios).listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY);
        orden.verify(horarios).guardar(any());
    }

    @Test
    void crearParaProfesionalInexistenteEs404SinGuardar() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(PROFESIONAL, DayOfWeek.MONDAY, NUEVE, TRECE))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(horarios, never()).guardar(any());
    }

    @Test
    void crearParaProfesionalInactivoNoSePermite() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(false)));

        assertThatThrownBy(() -> servicio.crear(PROFESIONAL, DayOfWeek.MONDAY, NUEVE, TRECE))
                .isInstanceOf(OperacionNoPermitidaException.class);
        verify(horarios, never()).guardar(any());
    }

    @Test
    void crearSuperpuestoEsConflictoSinGuardar() {
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY))
                .thenReturn(List.of(rango(10, DayOfWeek.MONDAY, "09:00", "13:00")));

        assertThatThrownBy(() -> servicio.crear(PROFESIONAL, DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(16, 0)))
                .isInstanceOf(ConflictoException.class);
        verify(horarios, never()).guardar(any());
    }

    @Test
    void editarExcluyeElPropioRangoYGuarda() {
        HorarioAtencion propio = rango(10, DayOfWeek.MONDAY, "09:00", "13:00");
        when(horarios.buscarPorId(10L)).thenReturn(Optional.of(propio));
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY)).thenReturn(List.of(propio));
        when(horarios.guardar(propio)).thenReturn(propio);

        HorarioAtencion editado = servicio.editar(10L, DayOfWeek.MONDAY, NUEVE, LocalTime.of(14, 0));

        assertThat(editado.getFin()).isEqualTo(LocalTime.of(14, 0));
        InOrder orden = inOrder(profesionales, horarios);
        orden.verify(profesionales).bloquearPorId(PROFESIONAL);
        orden.verify(horarios).listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY);
        orden.verify(horarios).guardar(propio);
    }

    @Test
    void editarConConflictoNoCambiaElRango() {
        HorarioAtencion propio = rango(10, DayOfWeek.MONDAY, "09:00", "13:00");
        HorarioAtencion tarde = rango(11, DayOfWeek.MONDAY, "14:00", "18:00");
        when(horarios.buscarPorId(10L)).thenReturn(Optional.of(propio));
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY)).thenReturn(List.of(propio, tarde));

        assertThatThrownBy(() -> servicio.editar(10L, DayOfWeek.MONDAY, NUEVE, LocalTime.of(15, 0)))
                .isInstanceOf(ConflictoException.class);
        assertThat(propio.getFin()).isEqualTo(TRECE);
        verify(horarios, never()).guardar(any());
    }

    @Test
    void editarAOtroDiaVerificaContraEseDia() {
        HorarioAtencion propio = rango(10, DayOfWeek.MONDAY, "09:00", "13:00");
        when(horarios.buscarPorId(10L)).thenReturn(Optional.of(propio));
        when(profesionales.bloquearPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.TUESDAY))
                .thenReturn(List.of(rango(12, DayOfWeek.TUESDAY, "10:00", "11:00")));

        assertThatThrownBy(() -> servicio.editar(10L, DayOfWeek.TUESDAY, NUEVE, TRECE))
                .isInstanceOf(ConflictoException.class);
        assertThat(propio.getDia()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    void editarOEliminarInexistenteEs404() {
        when(horarios.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.editar(99L, DayOfWeek.MONDAY, NUEVE, TRECE))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.eliminar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(horarios, never()).eliminar(any());
    }

    @Test
    void eliminarBorraElRango() {
        HorarioAtencion propio = rango(10, DayOfWeek.MONDAY, "09:00", "13:00");
        when(horarios.buscarPorId(10L)).thenReturn(Optional.of(propio));

        servicio.eliminar(10L);

        verify(horarios).eliminar(propio);
    }

    @Test
    void listarDeProfesionalInexistenteEs404() {
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.listarDelProfesional(PROFESIONAL))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listarDevuelveLosRangosDelRepositorio() {
        List<HorarioAtencion> rangos = List.of(rango(10, DayOfWeek.MONDAY, "09:00", "13:00"));
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.of(profesional(true)));
        when(horarios.listarDelProfesional(PROFESIONAL)).thenReturn(rangos);

        assertThat(servicio.listarDelProfesional(PROFESIONAL)).isEqualTo(rangos);
    }
}

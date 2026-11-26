package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class MotorDeDisponibilidadTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 30); // lunes
    private static final Instant AHORA = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(HOY, LocalTime.of(8, 0)));

    private final PrestacionRepositorio prestaciones = mock(PrestacionRepositorio.class);
    private final ProfesionalRepositorio profesionales = mock(ProfesionalRepositorio.class);
    private final SillonRepositorio sillones = mock(SillonRepositorio.class);
    private final HorarioAtencionRepositorio horarios = mock(HorarioAtencionRepositorio.class);
    private final BloqueoRepositorio bloqueos = mock(BloqueoRepositorio.class);
    private final TurnoRepositorio turnos = mock(TurnoRepositorio.class);
    private final Clock reloj = Clock.fixed(AHORA, ZoneOffset.UTC);
    private final ReglasDeConsultaDeHuecos reglas = new ReglasDeConsultaDeHuecos(15, 14);

    private final MotorDeDisponibilidad motor = new MotorDeDisponibilidad(
            prestaciones, profesionales, sillones, horarios, bloqueos, turnos, reloj, reglas);

    private static Prestacion prestacion(long id, boolean activa, int minutos) {
        Prestacion p = Prestacion.nueva("Consulta", minutos);
        ReflectionTestUtils.setField(p, "id", id);
        if (!activa) {
            p.desactivar();
        }
        return p;
    }

    private static Profesional profesional(long id, boolean activo, Long... prestacionIds) {
        Profesional p = Profesional.nuevo("Ana", "Gomez", null, "ana" + id + "@example.com");
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "prestacionIds", Set.of(prestacionIds));
        if (!activo) {
            p.desactivar();
        }
        return p;
    }

    private static Sillon sillon(long id) {
        Sillon s = Sillon.nuevo("Sillon " + id);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    @Test
    void rechazaPrestacionInexistenteOInactiva() {
        when(prestaciones.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(99L, null, HOY, HOY, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        when(prestaciones.buscarPorId(1L)).thenReturn(Optional.of(prestacion(1L, false, 30)));
        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(1L, null, HOY, HOY, null)))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void rechazaProfesionalElegidoInexistenteInactivoONoHabilitado() {
        when(prestaciones.buscarPorId(1L)).thenReturn(Optional.of(prestacion(1L, true, 30)));

        when(profesionales.buscarPorId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(1L, 99L, HOY, HOY, null)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        when(profesionales.buscarPorId(2L)).thenReturn(Optional.of(profesional(2L, false, 1L)));
        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(1L, 2L, HOY, HOY, null)))
                .isInstanceOf(ValidacionDeDominioException.class);

        when(profesionales.buscarPorId(3L)).thenReturn(Optional.of(profesional(3L, true, 2L)));
        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(1L, 3L, HOY, HOY, null)))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void rechazaRangoInvertidoODemasiadoLargoYDevuelveVacioSiTodoEsPasado() {
        when(prestaciones.buscarPorId(1L)).thenReturn(Optional.of(prestacion(1L, true, 30)));

        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(1L, null, HOY.plusDays(2), HOY, null)))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThatThrownBy(() -> motor.buscarHuecos(new ConsultaDeHuecos(1L, null, HOY, HOY.plusDays(14), null)))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThat(motor.buscarHuecos(new ConsultaDeHuecos(1L, null, HOY.minusDays(3), HOY.minusDays(1), null)))
                .isEmpty();
        verify(turnos, never()).listarOcupacionesDeProfesionales(any(), any(), any(), any());
    }

    @Test
    void recortaDesdePasadoAHoyYLeeUnaVezPorRecursoConRangoEnZonaDelConsultorio() {
        when(prestaciones.buscarPorId(1L)).thenReturn(Optional.of(prestacion(1L, true, 30)));
        Profesional profA = profesional(4L, true, 1L);
        Profesional profB = profesional(7L, true, 1L);
        when(profesionales.listarActivosQueRealizan(1L)).thenReturn(List.of(profA, profB));
        when(sillones.listarActivos()).thenReturn(List.of(sillon(10L)));
        when(horarios.listarDelProfesional(4L)).thenReturn(
                List.of(HorarioAtencion.nuevo(4L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0))));
        when(horarios.listarDelProfesional(7L)).thenReturn(List.of());
        when(bloqueos.listarQueAfectanAProfesional(anyLong(), any(), any())).thenReturn(List.of());
        when(bloqueos.listarQueAfectanASillon(anyLong(), any(), any())).thenReturn(List.of());
        when(turnos.listarOcupacionesDeProfesionales(any(), any(), any(), any())).thenReturn(List.of());
        when(turnos.listarOcupacionesDeSillones(any(), any(), any(), any())).thenReturn(List.of());

        List<Hueco> huecos = motor.buscarHuecos(new ConsultaDeHuecos(1L, null, HOY.minusDays(2), HOY, 42L));

        assertThat(huecos).hasSize(3);
        Instant desdeEsperado = Instant.parse("2026-11-30T03:00:00Z");
        Instant hastaEsperado = Instant.parse("2026-12-01T03:00:00Z");
        verify(horarios, times(1)).listarDelProfesional(4L);
        verify(horarios, times(1)).listarDelProfesional(7L);
        verify(bloqueos).listarQueAfectanAProfesional(4L, desdeEsperado, hastaEsperado);
        verify(bloqueos).listarQueAfectanASillon(10L, desdeEsperado, hastaEsperado);
        verify(turnos).listarOcupacionesDeProfesionales(List.of(4L, 7L), desdeEsperado, hastaEsperado, 42L);
        verify(turnos).listarOcupacionesDeSillones(List.of(10L), desdeEsperado, hastaEsperado, 42L);
        verify(profesionales, never()).bloquearPorId(anyLong());
        verify(sillones, never()).bloquearPorId(anyLong());
        verify(turnos, never()).guardar(any());
    }

    @Test
    void candidatosParaFuncionaFueraDeGrillaYDevuelveVacioSiInicioEsPasado() {
        when(prestaciones.buscarPorId(1L)).thenReturn(Optional.of(prestacion(1L, true, 30)));
        when(profesionales.listarActivosQueRealizan(1L)).thenReturn(List.of(profesional(4L, true, 1L)));
        when(sillones.listarActivos()).thenReturn(List.of(sillon(10L)));
        when(horarios.listarDelProfesional(4L)).thenReturn(
                List.of(HorarioAtencion.nuevo(4L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0))));
        when(bloqueos.listarQueAfectanAProfesional(anyLong(), any(), any())).thenReturn(List.of());
        when(bloqueos.listarQueAfectanASillon(anyLong(), any(), any())).thenReturn(List.of());
        when(turnos.listarOcupacionesDeProfesionales(any(), any(), any(), any())).thenReturn(List.of());
        when(turnos.listarOcupacionesDeSillones(any(), any(), any(), any())).thenReturn(List.of());

        Instant minutoFueraDeGrilla = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(HOY, LocalTime.of(9, 7)));
        assertThat(motor.candidatosPara(1L, null, minutoFueraDeGrilla, null))
                .containsExactly(new CandidatoDeReserva(4L, 10L));

        assertThat(motor.candidatosPara(1L, null, AHORA.minusSeconds(60), null)).isEmpty();
    }
}

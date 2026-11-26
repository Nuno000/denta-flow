package ar.dentaflow.aplicacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.BloqueoRepositorio;
import ar.dentaflow.dominio.disponibilidad.FranjaLocal;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Spec disponibilidad-recursos — lectura de los datos y delegación en la regla pura. Usa el día
 * del consultorio: el lunes 2026-11-30 va de {@code 2026-11-30T03:00Z} a {@code 2026-12-01T03:00Z}.
 */
@ExtendWith(MockitoExtension.class)
class ServicioDeDisponibilidadTest {

    private static final Long PROFESIONAL = 3L;
    private static final Long SILLON = 5L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final Instant INICIO_DEL_LUNES = Instant.parse("2026-11-30T03:00:00Z");
    private static final Instant FIN_DEL_LUNES = Instant.parse("2026-12-01T03:00:00Z");
    private static final Instant DIEZ = Instant.parse("2026-11-30T13:00:00Z");
    private static final Instant DIEZ_Y_MEDIA = Instant.parse("2026-11-30T13:30:00Z");

    @Mock
    private ProfesionalRepositorio profesionales;

    @Mock
    private SillonRepositorio sillones;

    @Mock
    private HorarioAtencionRepositorio horarios;

    @Mock
    private BloqueoRepositorio bloqueos;

    private DisponibilidadDeRecursos disponibilidad;

    @BeforeEach
    void preparar() {
        disponibilidad = new ServicioDeDisponibilidad(profesionales, sillones, horarios, bloqueos);
    }

    private static Profesional profesional() {
        return AyudanteDeIds.conId(Profesional.nuevo("Laura", "Ejemplo", null, "laura@example.com"), PROFESIONAL);
    }

    private static HorarioAtencion lunes(String inicio, String fin) {
        return HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    @Test
    void profesionalDisponibleLeeHorariosDelDiaLocalYBloqueosDelRango() {
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.of(profesional()));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY))
                .thenReturn(List.of(lunes("09:00", "13:00")));
        when(bloqueos.listarQueAfectanAProfesional(PROFESIONAL, DIEZ, DIEZ_Y_MEDIA)).thenReturn(List.of());

        assertThat(disponibilidad.profesionalDisponible(PROFESIONAL, DIEZ, DIEZ_Y_MEDIA)).isTrue();
    }

    @Test
    void profesionalConBloqueoNoEstaDisponible() {
        Bloqueo general = Bloqueo.delConsultorio(DIEZ, DIEZ_Y_MEDIA, MotivoBloqueo.FERIADO, null, 7L, DIEZ);
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.of(profesional()));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY))
                .thenReturn(List.of(lunes("09:00", "13:00")));
        when(bloqueos.listarQueAfectanAProfesional(PROFESIONAL, DIEZ, DIEZ_Y_MEDIA)).thenReturn(List.of(general));

        assertThat(disponibilidad.profesionalDisponible(PROFESIONAL, DIEZ, DIEZ_Y_MEDIA)).isFalse();
    }

    @Test
    void elDiaSeTomaDeLaZonaDelConsultorio() {
        Instant martesNocheEnUtc = Instant.parse("2026-12-01T23:30:00Z");
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.of(profesional()));

        disponibilidad.profesionalDisponible(PROFESIONAL, martesNocheEnUtc, martesNocheEnUtc.plusSeconds(1800));

        verify(horarios).listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.TUESDAY);
    }

    @Test
    void sillonDisponibleSegunSusBloqueos() {
        when(sillones.buscarPorId(SILLON)).thenReturn(Optional.of(AyudanteDeIds.conId(Sillon.nuevo("Sillón 1"), SILLON)));
        when(bloqueos.listarQueAfectanASillon(SILLON, DIEZ, DIEZ_Y_MEDIA)).thenReturn(List.of(
                Bloqueo.deSillon(SILLON, DIEZ, DIEZ_Y_MEDIA, MotivoBloqueo.OTRO, null, 7L, DIEZ)));

        assertThat(disponibilidad.sillonDisponible(SILLON, DIEZ, DIEZ_Y_MEDIA)).isFalse();
        verifyNoInteractions(horarios);
    }

    @Test
    void recursoInexistenteEs404() {
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.empty());
        when(sillones.buscarPorId(SILLON)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disponibilidad.profesionalDisponible(PROFESIONAL, DIEZ, DIEZ_Y_MEDIA))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> disponibilidad.sillonDisponible(SILLON, DIEZ, DIEZ_Y_MEDIA))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThatThrownBy(() -> disponibilidad.franjasDeAtencion(PROFESIONAL, LUNES))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void rangoInvalidoSeRechazaSinLeerLaBase() {
        assertThatThrownBy(() -> disponibilidad.profesionalDisponible(PROFESIONAL, DIEZ, DIEZ))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> disponibilidad.sillonDisponible(SILLON, DIEZ_Y_MEDIA, DIEZ))
                .isInstanceOf(ValidacionDeDominioException.class);
        verifyNoInteractions(profesionales, sillones, bloqueos);
    }

    @Test
    void franjasDeAtencionUnenLosContiguosDelDia() {
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.of(profesional()));
        when(horarios.listarDelProfesionalYDia(PROFESIONAL, DayOfWeek.MONDAY))
                .thenReturn(List.of(lunes("09:00", "13:00"), lunes("13:00", "15:00")));

        assertThat(disponibilidad.franjasDeAtencion(PROFESIONAL, LUNES))
                .containsExactly(new FranjaLocal(LUNES, LocalTime.of(9, 0), LocalTime.of(15, 0)));
    }

    @Test
    void bloqueosQueAfectanUsanLosLimitesDelDiaDelConsultorio() {
        when(profesionales.buscarPorId(PROFESIONAL)).thenReturn(Optional.of(profesional()));
        when(bloqueos.listarQueAfectanAProfesional(PROFESIONAL, INICIO_DEL_LUNES, FIN_DEL_LUNES)).thenReturn(List.of());

        assertThat(disponibilidad.bloqueosQueAfectan(PROFESIONAL, LUNES)).isEmpty();
        verify(bloqueos).listarQueAfectanAProfesional(PROFESIONAL, INICIO_DEL_LUNES, FIN_DEL_LUNES);
        verify(profesionales, never()).bloquearPorId(any());
    }
}

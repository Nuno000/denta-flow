package ar.dentaflow.dominio.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Spec disponibilidad-recursos completa. El lunes 2026-11-30 y el martes 2026-12-01 son del consultorio. */
class CalendarioDeDisponibilidadTest {

    private static final Long A = 1L;
    private static final Long B = 2L;
    private static final Long SILLON_1 = 10L;
    private static final Long SILLON_2 = 11L;
    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final Instant AHORA = Instant.parse("2026-11-25T12:00:00Z");

    private static Instant lunes(String hora) {
        return local(LUNES, hora);
    }

    private static Instant local(LocalDate fecha, String hora) {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(fecha, LocalTime.parse(hora)));
    }

    private static HorarioAtencion horario(DayOfWeek dia, String inicio, String fin) {
        return HorarioAtencion.nuevo(A, dia, LocalTime.parse(inicio), LocalTime.parse(fin));
    }

    private static List<HorarioAtencion> lunesDeNueveATrece() {
        return List.of(horario(DayOfWeek.MONDAY, "09:00", "13:00"));
    }

    private static boolean disponibleA(List<HorarioAtencion> horarios, List<Bloqueo> bloqueos, Instant inicio,
            Instant fin) {
        return CalendarioDeDisponibilidad.profesionalDisponible(A, true, horarios, bloqueos, inicio, fin);
    }

    private static Bloqueo bloqueoDeA(Instant inicio, Instant fin) {
        return Bloqueo.deProfesional(A, inicio, fin, MotivoBloqueo.AUSENCIA, null, 7L, AHORA);
    }

    private static Bloqueo bloqueoDelConsultorio(Instant inicio, Instant fin) {
        return Bloqueo.delConsultorio(inicio, fin, MotivoBloqueo.FERIADO, null, 7L, AHORA);
    }

    @Test
    void dentroDelHorario() {
        assertThat(disponibleA(lunesDeNueveATrece(), List.of(), lunes("10:00"), lunes("10:30"))).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"09:00, 09:30, true", "12:30, 13:00, true", "08:45, 09:15, false", "12:45, 13:15, false"})
    void bordesExactosDelHorario(String inicio, String fin, boolean esperado) {
        assertThat(disponibleA(lunesDeNueveATrece(), List.of(), lunes(inicio), lunes(fin))).isEqualTo(esperado);
    }

    @Test
    void rangosContiguosSeTratanComoContinuos() {
        List<HorarioAtencion> horarios = List.of(
                horario(DayOfWeek.MONDAY, "13:00", "17:00"), horario(DayOfWeek.MONDAY, "09:00", "13:00"));

        assertThat(disponibleA(horarios, List.of(), lunes("12:30"), lunes("13:30"))).isTrue();
    }

    @Test
    void huecoEntreRangosNoEstaDisponible() {
        List<HorarioAtencion> horarios = List.of(
                horario(DayOfWeek.MONDAY, "09:00", "13:00"), horario(DayOfWeek.MONDAY, "14:00", "18:00"));

        assertThat(disponibleA(horarios, List.of(), lunes("12:30"), lunes("14:30"))).isFalse();
    }

    @Test
    void diaSinHorario() {
        LocalDate domingo = LocalDate.of(2026, 11, 29);

        assertThat(disponibleA(lunesDeNueveATrece(), List.of(), local(domingo, "10:00"), local(domingo, "10:30")))
                .isFalse();
    }

    @Test
    void profesionalInactivo() {
        assertThat(CalendarioDeDisponibilidad.profesionalDisponible(
                A, false, lunesDeNueveATrece(), List.of(), lunes("10:00"), lunes("10:30"))).isFalse();
    }

    @Test
    void usaElDiaDelConsultorioYNoElDiaUtc() {
        List<HorarioAtencion> martes = List.of(horario(DayOfWeek.TUESDAY, "14:00", "22:00"));

        assertThat(disponibleA(martes, List.of(),
                Instant.parse("2026-12-01T23:30:00Z"), Instant.parse("2026-12-02T00:30:00Z"))).isTrue();
    }

    @Test
    void cruceDeMedianocheLocalNoEstaDisponible() {
        List<HorarioAtencion> horarios = List.of(
                horario(DayOfWeek.TUESDAY, "14:00", "23:59"), horario(DayOfWeek.WEDNESDAY, "00:00", "08:00"));

        assertThat(disponibleA(horarios, List.of(),
                local(LocalDate.of(2026, 12, 1), "23:30"), local(LocalDate.of(2026, 12, 2), "00:30"))).isFalse();
    }

    @Test
    void bloqueoParcialDelProfesional() {
        List<Bloqueo> bloqueos = List.of(bloqueoDeA(lunes("11:00"), lunes("12:00")));

        assertThat(disponibleA(lunesDeNueveATrece(), bloqueos, lunes("10:30"), lunes("11:30"))).isFalse();
    }

    @Test
    void bloqueoQueTerminaJustoAlInicio() {
        List<Bloqueo> bloqueos = List.of(bloqueoDeA(lunes("09:00"), lunes("10:00")));

        assertThat(disponibleA(lunesDeNueveATrece(), bloqueos, lunes("10:00"), lunes("10:30"))).isTrue();
    }

    @Test
    void bloqueoDelConsultorioQuitaATodos() {
        Bloqueo general = bloqueoDelConsultorio(lunes("00:00"), local(LUNES.plusDays(1), "00:00"));

        assertThat(disponibleA(lunesDeNueveATrece(), List.of(general), lunes("10:00"), lunes("10:30"))).isFalse();
        assertThat(CalendarioDeDisponibilidad.sillonDisponible(
                SILLON_1, true, List.of(general), lunes("10:00"), lunes("10:30"))).isFalse();
    }

    @Test
    void bloqueosDeOtrosRecursosNoAfectan() {
        List<Bloqueo> bloqueos = List.of(
                Bloqueo.deProfesional(B, lunes("09:00"), lunes("13:00"), MotivoBloqueo.AUSENCIA, null, 7L, AHORA),
                Bloqueo.deSillon(SILLON_2, lunes("09:00"), lunes("13:00"), MotivoBloqueo.OTRO, null, 7L, AHORA));

        assertThat(disponibleA(lunesDeNueveATrece(), bloqueos, lunes("10:00"), lunes("10:30"))).isTrue();
        assertThat(CalendarioDeDisponibilidad.sillonDisponible(
                SILLON_1, true, bloqueos, lunes("10:00"), lunes("10:30"))).isTrue();
    }

    @Test
    void sillonBloqueadoConProfesionalLibre() {
        List<Bloqueo> bloqueos = List.of(
                Bloqueo.deSillon(SILLON_1, lunes("10:00"), lunes("12:00"), MotivoBloqueo.OTRO, null, 7L, AHORA));

        assertThat(disponibleA(lunesDeNueveATrece(), bloqueos, lunes("10:00"), lunes("10:30"))).isTrue();
        assertThat(CalendarioDeDisponibilidad.sillonDisponible(
                SILLON_1, true, bloqueos, lunes("10:00"), lunes("10:30"))).isFalse();
    }

    @Test
    void sillonSinBloqueosActivoOInactivo() {
        assertThat(CalendarioDeDisponibilidad.sillonDisponible(
                SILLON_1, true, List.of(), lunes("03:00"), lunes("04:00"))).isTrue();
        assertThat(CalendarioDeDisponibilidad.sillonDisponible(
                SILLON_1, false, List.of(), lunes("10:00"), lunes("10:30"))).isFalse();
    }

    @Test
    void rangoVacioOInvertidoSeRechaza() {
        assertThatThrownBy(() -> disponibleA(lunesDeNueveATrece(), List.of(), lunes("10:00"), lunes("10:00")))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> CalendarioDeDisponibilidad.sillonDisponible(
                SILLON_1, true, List.of(), lunes("11:00"), lunes("10:00")))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> disponibleA(lunesDeNueveATrece(), List.of(), null, lunes("10:00")))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void franjasDelDiaUnenLosContiguos() {
        List<HorarioAtencion> horarios = List.of(
                horario(DayOfWeek.MONDAY, "16:00", "18:00"),
                horario(DayOfWeek.MONDAY, "13:00", "15:00"),
                horario(DayOfWeek.MONDAY, "09:00", "13:00"),
                horario(DayOfWeek.TUESDAY, "09:00", "20:00"));

        List<FranjaLocal> franjas = CalendarioDeDisponibilidad.franjasDelDia(horarios, LUNES);

        assertThat(franjas).containsExactly(
                new FranjaLocal(LUNES, LocalTime.of(9, 0), LocalTime.of(15, 0)),
                new FranjaLocal(LUNES, LocalTime.of(16, 0), LocalTime.of(18, 0)));
        assertThat(franjas.get(0).inicioInstante()).isEqualTo(Instant.parse("2026-11-30T12:00:00Z"));
        assertThat(franjas.get(0).finInstante()).isEqualTo(Instant.parse("2026-11-30T18:00:00Z"));
    }
}

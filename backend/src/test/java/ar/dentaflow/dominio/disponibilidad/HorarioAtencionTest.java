package ar.dentaflow.dominio.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Spec horarios-atencion — "Rango semanal en hora del consultorio" y "Sin rangos superpuestos". */
class HorarioAtencionTest {

    private static final Long PROFESIONAL = 1L;

    private static LocalTime hora(String texto) {
        return LocalTime.parse(texto);
    }

    private static HorarioAtencion rango(long id, Long profesional, DayOfWeek dia, String inicio, String fin) {
        return AyudanteDeIds.conId(HorarioAtencion.nuevo(profesional, dia, hora(inicio), hora(fin)), id);
    }

    @Test
    void altaValidaGuardaLosDatos() {
        HorarioAtencion horario = HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, hora("09:00"), hora("13:00"));

        assertThat(horario.getProfesionalId()).isEqualTo(PROFESIONAL);
        assertThat(horario.getDia()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(horario.getInicio()).isEqualTo(hora("09:00"));
        assertThat(horario.getFin()).isEqualTo(hora("13:00"));
    }

    @ParameterizedTest
    @CsvSource({"13:00, 13:00", "18:00, 09:00", "09:00:30, 13:00", "09:00, 12:59:59", "09:00:00.5, 13:00"})
    void rechazaRangosInvalidos(String inicio, String fin) {
        assertThatThrownBy(() -> HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, hora(inicio), hora(fin)))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void rechazaDatosFaltantes() {
        assertThatThrownBy(() -> HorarioAtencion.nuevo(null, DayOfWeek.MONDAY, hora("09:00"), hora("13:00")))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("profesional");
        assertThatThrownBy(() -> HorarioAtencion.nuevo(PROFESIONAL, null, hora("09:00"), hora("13:00")))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("día");
        assertThatThrownBy(() -> HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, null, hora("13:00")))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("inicio");
        assertThatThrownBy(() -> HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, hora("09:00"), null))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("fin");
    }

    @ParameterizedTest
    @CsvSource({"12:00, 16:00", "10:00, 11:00", "08:00, 09:30", "08:00, 14:00"})
    void rechazaSuperposicionConUnRangoDelMismoDia(String inicio, String fin) {
        List<HorarioAtencion> existentes = List.of(rango(10, PROFESIONAL, DayOfWeek.MONDAY, "09:00", "13:00"));
        HorarioAtencion nuevo = HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, hora(inicio), hora(fin));

        assertThatThrownBy(() -> HorarioAtencion.verificarSinSuperposicion(nuevo, existentes))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("09:00").hasMessageContaining("13:00");
    }

    @Test
    void aceptaContiguosOtroDiaYOtroProfesional() {
        List<HorarioAtencion> existentes = List.of(rango(10, PROFESIONAL, DayOfWeek.MONDAY, "09:00", "13:00"));

        HorarioAtencion.verificarSinSuperposicion(
                HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, hora("13:00"), hora("17:00")), existentes);
        HorarioAtencion.verificarSinSuperposicion(
                HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.MONDAY, hora("07:00"), hora("09:00")), existentes);
        HorarioAtencion.verificarSinSuperposicion(
                HorarioAtencion.nuevo(PROFESIONAL, DayOfWeek.TUESDAY, hora("09:00"), hora("13:00")), existentes);
        HorarioAtencion.verificarSinSuperposicion(
                HorarioAtencion.nuevo(2L, DayOfWeek.MONDAY, hora("09:00"), hora("13:00")), existentes);
    }

    @Test
    void laEdicionNoChocaConsigoMisma() {
        HorarioAtencion propio = rango(10, PROFESIONAL, DayOfWeek.MONDAY, "09:00", "13:00");
        List<HorarioAtencion> existentes = List.of(propio);

        propio.editar(DayOfWeek.MONDAY, hora("09:00"), hora("14:00"));

        HorarioAtencion.verificarSinSuperposicion(propio, existentes);
        assertThat(propio.getFin()).isEqualTo(hora("14:00"));
    }

    @Test
    void laEdicionQueChocaConOtroRangoSeRechaza() {
        HorarioAtencion primero = rango(10, PROFESIONAL, DayOfWeek.MONDAY, "09:00", "13:00");
        HorarioAtencion segundo = rango(11, PROFESIONAL, DayOfWeek.MONDAY, "14:00", "18:00");
        HorarioAtencion editado = rango(10, PROFESIONAL, DayOfWeek.MONDAY, "09:00", "15:00");

        assertThatThrownBy(() -> HorarioAtencion.verificarSinSuperposicion(editado, List.of(primero, segundo)))
                .isInstanceOf(ConflictoException.class).hasMessageContaining("14:00");
    }

    @Test
    void edicionInvalidaNoCambiaNada() {
        HorarioAtencion horario = rango(10, PROFESIONAL, DayOfWeek.MONDAY, "09:00", "13:00");

        assertThatThrownBy(() -> horario.editar(DayOfWeek.TUESDAY, hora("15:00"), hora("10:00")))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(horario.getDia()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(horario.getInicio()).isEqualTo(hora("09:00"));
        assertThat(horario.getFin()).isEqualTo(hora("13:00"));
    }
}

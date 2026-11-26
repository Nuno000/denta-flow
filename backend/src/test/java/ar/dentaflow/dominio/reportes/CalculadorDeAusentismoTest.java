package ar.dentaflow.dominio.reportes;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.turnos.EstadoTurno;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Spec panel-ausentismo — cálculo puro de tasas, franjas, días, profesionales y pacientes reiterados (US-031). */
class CalculadorDeAusentismoTest {

    private static final LocalDate DESDE = LocalDate.of(2031, 3, 1);
    private static final LocalDate HASTA = LocalDate.of(2031, 3, 31);

    private static RegistroAsistenciaParaReporte registro(String instanteUtc, EstadoTurno estado,
            long profId, String profNombre, String profApellido,
            long pacId, String pacNombre, String pacApellido) {
        return new RegistroAsistenciaParaReporte(
                Instant.parse(instanteUtc), estado, profId, profNombre, profApellido, pacId, pacNombre, pacApellido);
    }

    @Test
    void sin_turnos_evaluados_devuelve_tasas_en_cero_y_franjas_vacias() {
        ReporteDeAusentismo reporte = CalculadorDeAusentismo.calcular(List.of(), DESDE, HASTA, null, 2);

        assertThat(reporte.general().totalEvaluados()).isZero();
        assertThat(reporte.general().totalAusentes()).isZero();
        assertThat(reporte.general().tasaAusentismo()).isEqualTo(0.0);
        assertThat(reporte.porProfesional()).isEmpty();
        assertThat(reporte.porDiaSemana()).isEmpty();
        assertThat(reporte.pacientesReiterados()).isEmpty();
        assertThat(reporte.porFranja())
                .extracting(AusentismoPorFranja::franja)
                .containsExactly(FranjaHoraria.MANANA, FranjaHoraria.TARDE, FranjaHoraria.NOCHE);
        assertThat(reporte.porFranja())
                .allSatisfy(f -> assertThat(f.resumen().totalEvaluados()).isZero());
    }

    @Test
    void excluye_estados_no_evaluados_y_calcula_tasa_con_un_decimal() {
        List<RegistroAsistenciaParaReporte> registros = List.of(
                registro("2031-03-04T13:00:00Z", EstadoTurno.ASISTIO, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                registro("2031-03-04T14:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                registro("2031-03-04T15:00:00Z", EstadoTurno.AUSENTE_CON_AVISO, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                registro("2031-03-04T16:00:00Z", EstadoTurno.PENDIENTE, 1L, "Laura", "Gómez", 11L, "Juan", "Pérez"),
                registro("2031-03-04T17:00:00Z", EstadoTurno.CONFIRMADO, 1L, "Laura", "Gómez", 11L, "Juan", "Pérez"),
                registro("2031-03-04T18:00:00Z", EstadoTurno.CANCELADO, 1L, "Laura", "Gómez", 11L, "Juan", "Pérez"));

        ReporteDeAusentismo reporte = CalculadorDeAusentismo.calcular(registros, DESDE, HASTA, null, 2);

        assertThat(reporte.general().totalEvaluados()).isEqualTo(3);
        assertThat(reporte.general().asistidos()).isEqualTo(1);
        assertThat(reporte.general().ausentesSinAviso()).isEqualTo(1);
        assertThat(reporte.general().ausentesConAviso()).isEqualTo(1);
        assertThat(reporte.general().totalAusentes()).isEqualTo(2);
        assertThat(reporte.general().tasaAusentismo()).isEqualTo(66.7);
    }

    @Test
    void clasifica_franjas_y_dia_de_la_semana_en_hora_argentina() {
        // 2031-03-03 es lunes en Argentina.
        // 13:00Z -> lunes 10:00 (Mañana)
        // 17:30Z -> lunes 14:30 (Tarde)
        // 22:00Z -> lunes 19:00 (Noche)
        // 2031-03-04T02:30Z -> lunes 23:30 en Argentina (Noche, sigue siendo LUNES!)
        List<RegistroAsistenciaParaReporte> registros = List.of(
                registro("2031-03-03T13:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                registro("2031-03-03T17:30:00Z", EstadoTurno.ASISTIO, 1L, "Laura", "Gómez", 11L, "Juan", "Pérez"),
                registro("2031-03-03T22:00:00Z", EstadoTurno.AUSENTE_CON_AVISO, 1L, "Laura", "Gómez", 12L, "Sofía", "Ruiz"),
                registro("2031-03-04T02:30:00Z", EstadoTurno.ASISTIO, 1L, "Laura", "Gómez", 13L, "Diego", "Vera"));

        ReporteDeAusentismo reporte = CalculadorDeAusentismo.calcular(registros, DESDE, HASTA, null, 2);

        assertThat(reporte.porFranja()).hasSize(3);
        assertThat(reporte.porFranja().get(0).franja()).isEqualTo(FranjaHoraria.MANANA);
        assertThat(reporte.porFranja().get(0).resumen().tasaAusentismo()).isEqualTo(100.0);
        assertThat(reporte.porFranja().get(1).franja()).isEqualTo(FranjaHoraria.TARDE);
        assertThat(reporte.porFranja().get(1).resumen().tasaAusentismo()).isEqualTo(0.0);
        assertThat(reporte.porFranja().get(2).franja()).isEqualTo(FranjaHoraria.NOCHE);
        assertThat(reporte.porFranja().get(2).resumen().totalEvaluados()).isEqualTo(2);
        assertThat(reporte.porFranja().get(2).resumen().totalAusentes()).isEqualTo(1);
        assertThat(reporte.porFranja().get(2).resumen().tasaAusentismo()).isEqualTo(50.0);

        assertThat(reporte.porDiaSemana()).hasSize(1);
        assertThat(reporte.porDiaSemana().get(0).diaSemana()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(reporte.porDiaSemana().get(0).etiqueta()).isEqualTo("Lunes");
        assertThat(reporte.porDiaSemana().get(0).resumen().totalEvaluados()).isEqualTo(4);
    }

    @Test
    void ordena_profesionales_por_tasa_y_ausencias_descendentes() {
        List<RegistroAsistenciaParaReporte> registros = List.of(
                // Dra. B: 1/4 = 25.0 %
                registro("2031-03-04T13:00:00Z", EstadoTurno.AUSENTE, 2L, "Beatriz", "Bustos", 10L, "Ana", "López"),
                registro("2031-03-04T14:00:00Z", EstadoTurno.ASISTIO, 2L, "Beatriz", "Bustos", 11L, "Juan", "Pérez"),
                registro("2031-03-04T15:00:00Z", EstadoTurno.ASISTIO, 2L, "Beatriz", "Bustos", 12L, "Sofía", "Ruiz"),
                registro("2031-03-04T16:00:00Z", EstadoTurno.ASISTIO, 2L, "Beatriz", "Bustos", 13L, "Diego", "Vera"),
                // Dr. A: 1/2 = 50.0 %
                registro("2031-03-04T13:00:00Z", EstadoTurno.AUSENTE, 1L, "Andrés", "Álvarez", 10L, "Ana", "López"),
                registro("2031-03-04T14:00:00Z", EstadoTurno.ASISTIO, 1L, "Andrés", "Álvarez", 11L, "Juan", "Pérez"));

        ReporteDeAusentismo reporte = CalculadorDeAusentismo.calcular(registros, DESDE, HASTA, null, 2);

        assertThat(reporte.porProfesional())
                .extracting(AusentismoPorProfesional::profesionalId)
                .containsExactly(1L, 2L);
        assertThat(reporte.porProfesional().get(0).resumen().tasaAusentismo()).isEqualTo(50.0);
        assertThat(reporte.porProfesional().get(1).resumen().tasaAusentismo()).isEqualTo(25.0);
    }

    @Test
    void filtra_y_ordena_pacientes_con_ausencias_reiteradas_segun_umbral() {
        List<RegistroAsistenciaParaReporte> registros = List.of(
                // Paciente A: 2 ausentes de 3 (66.7 %)
                registro("2031-03-04T13:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                registro("2031-03-05T13:00:00Z", EstadoTurno.AUSENTE_CON_AVISO, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                registro("2031-03-06T13:00:00Z", EstadoTurno.ASISTIO, 1L, "Laura", "Gómez", 10L, "Ana", "López"),
                // Paciente B: 1 ausente de 2 (50.0 %) -> debajo del umbral 2
                registro("2031-03-04T14:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 11L, "Juan", "Pérez"),
                registro("2031-03-05T14:00:00Z", EstadoTurno.ASISTIO, 1L, "Laura", "Gómez", 11L, "Juan", "Pérez"),
                // Paciente C: 3 ausentes de 3 (100.0 %) -> primero
                registro("2031-03-04T15:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 12L, "Carlos", "Díaz"),
                registro("2031-03-05T15:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 12L, "Carlos", "Díaz"),
                registro("2031-03-06T15:00:00Z", EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 12L, "Carlos", "Díaz"));

        ReporteDeAusentismo reporte = CalculadorDeAusentismo.calcular(registros, DESDE, HASTA, null, 2);

        assertThat(reporte.pacientesReiterados())
                .extracting(AusentismoPorPaciente::pacienteId)
                .containsExactly(12L, 10L);
        assertThat(reporte.pacientesReiterados().get(0).resumen().totalAusentes()).isEqualTo(3);
        assertThat(reporte.pacientesReiterados().get(1).resumen().totalAusentes()).isEqualTo(2);
        assertThat(reporte.pacientesReiterados().get(1).resumen().tasaAusentismo()).isEqualTo(66.7);
    }
}

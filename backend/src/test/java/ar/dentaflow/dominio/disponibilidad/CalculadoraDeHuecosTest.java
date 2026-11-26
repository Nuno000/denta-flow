package ar.dentaflow.dominio.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango.ProfesionalEnAgenda;
import ar.dentaflow.dominio.disponibilidad.AgendaDelRango.SillonEnAgenda;
import ar.dentaflow.dominio.turnos.OcupacionDeRecurso;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraDeHuecosTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final LocalDate MARTES = LocalDate.of(2026, 12, 1);
    private static final LocalDate MIERCOLES = LocalDate.of(2026, 12, 2);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 12, 6);

    private static Instant instante(LocalDate fecha, int hora, int minuto) {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(fecha, LocalTime.of(hora, minuto)));
    }

    private static ProfesionalEnAgenda profesional(Long id, boolean activo, HorarioAtencion... horarios) {
        return new ProfesionalEnAgenda(id, activo, List.of(horarios), List.of());
    }

    private static ProfesionalEnAgenda profesionalConBloqueos(Long id, List<HorarioAtencion> horarios,
            List<Bloqueo> bloqueos) {
        return new ProfesionalEnAgenda(id, true, horarios, bloqueos);
    }

    private static SillonEnAgenda sillon(Long id, boolean activo, Bloqueo... bloqueos) {
        return new SillonEnAgenda(id, activo, List.of(bloqueos));
    }

    @Test
    void excluye_inicios_donde_no_entra_la_duracion_completa_en_la_franja() {
        var prof = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        var agenda = new AgendaDelRango(45, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(10L, true)), List.of(), List.of());

        List<Hueco> huecos = CalculadoraDeHuecos.calcular(agenda);

        assertThat(huecos).extracting(Hueco::inicio)
                .containsExactly(instante(LUNES, 9, 0), instante(LUNES, 9, 15));
    }

    @Test
    void ofrece_el_borde_exacto_de_la_franja_y_nada_despues() {
        var prof = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        var agenda = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(10L, true)), List.of(), List.of());

        List<Hueco> huecos = CalculadoraDeHuecos.calcular(agenda);

        assertThat(huecos).isNotEmpty();
        Hueco ultimo = huecos.get(huecos.size() - 1);
        assertThat(ultimo.inicio()).isEqualTo(instante(LUNES, 12, 30));
        assertThat(ultimo.fin()).isEqualTo(instante(LUNES, 13, 0));
    }

    @Test
    void trata_franjas_contiguas_como_continuas_y_calcula_fin_como_instante() {
        var prof = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)),
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(17, 0)));
        var agenda = new AgendaDelRango(60, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(10L, true)), List.of(), List.of());

        List<Hueco> huecos = CalculadoraDeHuecos.calcular(agenda);

        assertThat(huecos).anySatisfy(h -> {
            assertThat(h.inicio()).isEqualTo(instante(LUNES, 12, 30));
            assertThat(h.fin()).isEqualTo(instante(LUNES, 13, 30));
        });
    }

    @Test
    void dia_sin_horario_no_ofrece_ningun_hueco() {
        var prof = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        var agenda = new AgendaDelRango(30, DOMINGO, DOMINGO, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(10L, true)), List.of(), List.of());

        assertThat(CalculadoraDeHuecos.calcular(agenda)).isEmpty();
    }

    @Test
    void bloqueo_parcial_del_profesional_quita_solo_los_huecos_superpuestos() {
        var bloqueo = Bloqueo.deProfesional(1L, instante(LUNES, 11, 0), instante(LUNES, 12, 0),
                MotivoBloqueo.AUSENCIA, null, 99L, instante(LUNES, 7, 0));
        var prof = profesionalConBloqueos(1L,
                List.of(HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))),
                List.of(bloqueo));
        var agenda = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(10L, true)), List.of(), List.of());

        List<Instant> inicios = CalculadoraDeHuecos.calcular(agenda).stream().map(Hueco::inicio).toList();

        assertThat(inicios).contains(instante(LUNES, 10, 30), instante(LUNES, 12, 0));
        assertThat(inicios).doesNotContain(
                instante(LUNES, 10, 45),
                instante(LUNES, 11, 0),
                instante(LUNES, 11, 15),
                instante(LUNES, 11, 30),
                instante(LUNES, 11, 45));
    }

    @Test
    void bloqueo_del_consultorio_quita_todo_el_dia_y_profesional_inactivo_no_ofrece_huecos() {
        var bloqueoConsultorio = Bloqueo.delConsultorio(instante(LUNES, 0, 0), instante(MARTES, 0, 0),
                MotivoBloqueo.FERIADO, "Feriado", 99L, instante(LUNES, 0, 0));
        var profActivo = profesionalConBloqueos(1L,
                List.of(HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))),
                List.of(bloqueoConsultorio));
        var profInactivo = profesional(2L, false,
                HorarioAtencion.nuevo(2L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));

        var agendaBloqueada = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(profActivo), List.of(sillon(10L, true, bloqueoConsultorio)), List.of(), List.of());
        var agendaInactivo = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(profInactivo), List.of(sillon(10L, true)), List.of(), List.of());

        assertThat(CalculadoraDeHuecos.calcular(agendaBloqueada)).isEmpty();
        assertThat(CalculadoraDeHuecos.calcular(agendaInactivo)).isEmpty();
    }

    @Test
    void bloqueo_de_un_sillon_asigna_el_otro_sillon_libre() {
        var bloqueoSillon1 = Bloqueo.deSillon(1L, instante(LUNES, 10, 0), instante(LUNES, 12, 0),
                MotivoBloqueo.OTRO, "Mantenimiento", 99L, instante(LUNES, 7, 0));
        var prof = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        var agenda = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof),
                List.of(sillon(1L, true, bloqueoSillon1), sillon(2L, true)),
                List.of(), List.of());

        List<Hueco> huecos = CalculadoraDeHuecos.calcular(agenda);

        assertThat(huecos).hasSize(3);
        assertThat(huecos.get(0).candidatos()).containsExactly(new CandidatoDeReserva(1L, 2L));
    }

    @Test
    void ocupaciones_quitan_huecos_intermedios_respetan_bordes_y_requieren_sillon_libre() {
        var prof = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        var ocupacionesProf = List.of(
                new OcupacionDeRecurso(1L, instante(LUNES, 9, 0), instante(LUNES, 9, 30)),
                new OcupacionDeRecurso(1L, instante(LUNES, 10, 0), instante(LUNES, 10, 30)));
        var agenda45 = new AgendaDelRango(45, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(1L, true)), ocupacionesProf, List.of());

        List<Instant> inicios45 = CalculadoraDeHuecos.calcular(agenda45).stream().map(Hueco::inicio).toList();
        assertThat(inicios45).doesNotContain(
                instante(LUNES, 9, 15), instante(LUNES, 9, 30), instante(LUNES, 9, 45));

        var agenda30 = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(1L, true)),
                List.of(new OcupacionDeRecurso(1L, instante(LUNES, 9, 0), instante(LUNES, 9, 30))),
                List.of());
        assertThat(CalculadoraDeHuecos.calcular(agenda30)).extracting(Hueco::inicio)
                .contains(instante(LUNES, 9, 30));

        // Único sillón ocupado por otro profesional mientras A está libre -> sin hueco a las 10:00
        var agendaSillonOcupado = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof), List.of(sillon(1L, true)),
                List.of(),
                List.of(new OcupacionDeRecurso(1L, instante(LUNES, 10, 0), instante(LUNES, 10, 30))));
        assertThat(CalculadoraDeHuecos.calcular(agendaSillonOcupado)).extracting(Hueco::inicio)
                .doesNotContain(instante(LUNES, 10, 0));
    }

    @Test
    void ordena_candidatos_por_profesional_y_sillon_y_agrupa_sin_duplicar_inicios() {
        var prof4 = profesional(4L, true,
                HorarioAtencion.nuevo(4L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(10, 30)));
        var prof7 = profesional(7L, true,
                HorarioAtencion.nuevo(7L, DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(11, 0)));
        var ocupacionSillon1 = new OcupacionDeRecurso(1L, instante(LUNES, 10, 0), instante(LUNES, 10, 30));
        var agenda = new AgendaDelRango(30, LUNES, LUNES, instante(LUNES, 8, 0), 15,
                List.of(prof4, prof7),
                List.of(sillon(1L, true), sillon(2L, true), sillon(3L, true)),
                List.of(),
                List.of(ocupacionSillon1));

        List<Hueco> huecos = CalculadoraDeHuecos.calcular(agenda);

        assertThat(huecos).extracting(Hueco::inicio)
                .containsExactly(instante(LUNES, 10, 0), instante(LUNES, 10, 15), instante(LUNES, 10, 30));
        assertThat(huecos.get(0).candidatos()).containsExactly(
                new CandidatoDeReserva(4L, 2L),
                new CandidatoDeReserva(4L, 3L),
                new CandidatoDeReserva(7L, 2L),
                new CandidatoDeReserva(7L, 3L));
        assertThat(CalculadoraDeHuecos.candidatosEn(agenda, instante(LUNES, 10, 0)))
                .isEqualTo(huecos.get(0).candidatos());
    }

    @Test
    void excluye_huecos_pasados_y_respeta_zona_horaria_cuando_cambia_el_dia_utc() {
        Instant ahoraLunes = Instant.parse("2026-11-30T13:10:00Z"); // 10:10 ART
        var profLunes = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        var agendaLunes = new AgendaDelRango(30, LUNES, LUNES, ahoraLunes, 15,
                List.of(profLunes), List.of(sillon(1L, true)), List.of(), List.of());

        List<Hueco> huecosLunes = CalculadoraDeHuecos.calcular(agendaLunes);
        assertThat(huecosLunes.get(0).inicio()).isEqualTo(instante(LUNES, 10, 15));

        // Martes 20:00-22:00 -> 21:30 ART = 2026-12-02T00:30:00Z (miércoles en UTC)
        var profMartes = profesional(1L, true,
                HorarioAtencion.nuevo(1L, DayOfWeek.TUESDAY, LocalTime.of(20, 0), LocalTime.of(22, 0)));
        var agendaMartes = new AgendaDelRango(30, MARTES, MIERCOLES, ahoraLunes, 15,
                List.of(profMartes), List.of(sillon(1L, true)), List.of(), List.of());

        List<Hueco> huecosMartes = CalculadoraDeHuecos.calcular(agendaMartes);
        assertThat(huecosMartes).extracting(Hueco::inicio)
                .contains(Instant.parse("2026-12-02T00:30:00Z"))
                .allMatch(i -> ZonaHorariaConsultorio.aHoraLocal(i).toLocalDate().equals(MARTES));

        // Ocupación en 2026-12-02T00:30Z bloquea las 21:30 del martes
        var agendaOcupada = new AgendaDelRango(30, MARTES, MARTES, ahoraLunes, 15,
                List.of(profMartes), List.of(sillon(1L, true)),
                List.of(new OcupacionDeRecurso(1L,
                        Instant.parse("2026-12-02T00:30:00Z"),
                        Instant.parse("2026-12-02T01:00:00Z"))),
                List.of());
        assertThat(CalculadoraDeHuecos.calcular(agendaOcupada)).extracting(Hueco::inicio)
                .doesNotContain(Instant.parse("2026-12-02T00:30:00Z"));
    }
}

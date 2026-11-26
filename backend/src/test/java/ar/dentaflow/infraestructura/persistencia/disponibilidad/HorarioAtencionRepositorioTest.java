package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Mapeo de {@code horario_atencion}: ida y vuelta de día y horas, orden y borrado. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HorarioAtencionRepositorioTest {

    @Autowired
    private HorarioAtencionRepositorio horarios;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private Long profesional;
    private Long otro;

    @BeforeEach
    void crearProfesionales() {
        profesional = profesionales.guardar(Profesional.nuevo("Laura", "Ejemplo", null, "laura.h@example.com")).getId();
        otro = profesionales.guardar(Profesional.nuevo("Martín", "Prueba", null, "martin.h@example.com")).getId();
    }

    private HorarioAtencion guardar(Long profesionalId, DayOfWeek dia, String inicio, String fin) {
        return horarios.guardar(HorarioAtencion.nuevo(profesionalId, dia, LocalTime.parse(inicio), LocalTime.parse(fin)));
    }

    @Test
    void idaYVueltaConservaDiaYHoras() {
        Long id = guardar(profesional, DayOfWeek.SUNDAY, "09:30", "13:45").getId();
        em.flush();
        em.clear();

        HorarioAtencion leido = horarios.buscarPorId(id).orElseThrow();

        assertThat(leido.getProfesionalId()).isEqualTo(profesional);
        assertThat(leido.getDia()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(leido.getInicio()).isEqualTo(LocalTime.of(9, 30));
        assertThat(leido.getFin()).isEqualTo(LocalTime.of(13, 45));
        assertThat(jdbc.queryForObject("SELECT dia_semana FROM horario_atencion WHERE id = ?", Integer.class, id))
                .isEqualTo(7);
    }

    @Test
    void listaOrdenadoPorDiaYHoraYFiltraPorDia() {
        guardar(profesional, DayOfWeek.TUESDAY, "09:00", "13:00");
        guardar(profesional, DayOfWeek.MONDAY, "14:00", "18:00");
        guardar(profesional, DayOfWeek.MONDAY, "09:00", "13:00");
        guardar(otro, DayOfWeek.MONDAY, "08:00", "09:00");
        em.flush();
        em.clear();

        List<HorarioAtencion> todos = horarios.listarDelProfesional(profesional);
        List<HorarioAtencion> lunes = horarios.listarDelProfesionalYDia(profesional, DayOfWeek.MONDAY);

        assertThat(todos).extracting(h -> h.getDia() + " " + h.getInicio())
                .containsExactly("MONDAY 09:00", "MONDAY 14:00", "TUESDAY 09:00");
        assertThat(lunes).extracting(HorarioAtencion::getInicio)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(14, 0));
    }

    @Test
    void eliminarBorraElRango() {
        HorarioAtencion horario = guardar(profesional, DayOfWeek.MONDAY, "09:00", "13:00");
        em.flush();

        horarios.eliminar(horario);
        em.flush();
        em.clear();

        assertThat(horarios.buscarPorId(horario.getId())).isEmpty();
    }
}

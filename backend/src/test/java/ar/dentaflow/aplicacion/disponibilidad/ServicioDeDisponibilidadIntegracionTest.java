package ar.dentaflow.aplicacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec disponibilidad-recursos de punta a punta sobre H2: horarios y bloqueos guardados por sus
 * servicios y consultados con {@link DisponibilidadDeRecursos}. Lunes 2026-11-30, hora del consultorio.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServicioDeDisponibilidadIntegracionTest {

    @Autowired
    private DisponibilidadDeRecursos disponibilidad;

    @Autowired
    private ServicioDeHorarios servicioDeHorarios;

    @Autowired
    private ServicioDeBloqueos servicioDeBloqueos;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private SillonRepositorio sillones;

    @Autowired
    private JdbcTemplate jdbc;

    private static Instant lunes(String hora) {
        return OffsetDateTime.parse("2026-11-30T" + hora + ":00-03:00").toInstant();
    }

    @Test
    void horarioMenosBloqueosDeProfesionalYSillon() {
        jdbc.update("INSERT INTO usuario (email, credencial_hash, rol) VALUES ('admin.disp@example.com', 'x', 'administrador')");
        Long admin = jdbc.queryForObject("SELECT id FROM usuario WHERE email = 'admin.disp@example.com'", Long.class);
        Long profesional = profesionales.guardar(
                Profesional.nuevo("Laura", "Disponible", null, "laura.disp@example.com")).getId();
        Long sillonBloqueado = sillones.guardar(Sillon.nuevo("Sillón disp 1")).getId();
        Long sillonLibre = sillones.guardar(Sillon.nuevo("Sillón disp 2")).getId();
        servicioDeHorarios.crear(profesional, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));
        servicioDeBloqueos.crear(AmbitoBloqueo.CONSULTORIO, null, null, lunes("11:00"), lunes("12:00"),
                MotivoBloqueo.OTRO, "Reunión de equipo", admin);
        servicioDeBloqueos.crear(AmbitoBloqueo.SILLON, null, sillonBloqueado, lunes("09:00"), lunes("10:00"),
                MotivoBloqueo.OTRO, "Mantenimiento", admin);

        assertThat(disponibilidad.profesionalDisponible(profesional, lunes("10:00"), lunes("10:30"))).isTrue();
        assertThat(disponibilidad.profesionalDisponible(profesional, lunes("11:30"), lunes("12:00"))).isFalse();
        assertThat(disponibilidad.sillonDisponible(sillonBloqueado, lunes("09:30"), lunes("10:00"))).isFalse();
        assertThat(disponibilidad.sillonDisponible(sillonLibre, lunes("09:30"), lunes("10:00"))).isTrue();
        assertThat(disponibilidad.bloqueosQueAfectan(profesional, LocalDate.of(2026, 11, 30)))
                .singleElement().satisfies(b -> assertThat(b.getAmbito()).isEqualTo(AmbitoBloqueo.CONSULTORIO));
    }
}

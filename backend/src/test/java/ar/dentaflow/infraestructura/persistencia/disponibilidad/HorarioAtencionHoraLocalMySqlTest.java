package ar.dentaflow.infraestructura.persistencia.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import jakarta.persistence.EntityManager;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifica contra MySQL 8 real que {@code horario_atencion} persiste la hora de reloj sin
 * desplazamiento UTC aunque la JVM esté en otra zona (por ejemplo, {@code 20:00–22:00} en
 * Argentina no cruza la medianoche ni viola {@code ck_horario_atencion_rango}).
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
@Transactional
class HorarioAtencionHoraLocalMySqlTest {

    @Autowired
    private HorarioAtencionRepositorio horarios;

    @Autowired
    private ProfesionalRepositorio profesionales;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void guardaYReleeHoraLocalSinDesplazamientoEntreZonas() {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
            String email = "prof-hora-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
            Profesional profesional = profesionales.guardar(Profesional.nuevo("Sofía", "Reloj", null, email));
            Long id = horarios.guardar(HorarioAtencion.nuevo(profesional.getId(), DayOfWeek.FRIDAY,
                    LocalTime.of(20, 0), LocalTime.of(22, 0))).getId();
            em.flush();
            em.clear();

            assertThat(jdbc.queryForObject("SELECT CAST(hora_inicio AS CHAR(8)) FROM horario_atencion WHERE id = ?",
                    String.class, id)).isEqualTo("20:00:00");
            assertThat(jdbc.queryForObject("SELECT CAST(hora_fin AS CHAR(8)) FROM horario_atencion WHERE id = ?",
                    String.class, id)).isEqualTo("22:00:00");

            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            HorarioAtencion leido = horarios.buscarPorId(id).orElseThrow();
            assertThat(leido.getInicio()).isEqualTo(LocalTime.of(20, 0));
            assertThat(leido.getFin()).isEqualTo(LocalTime.of(22, 0));
        } finally {
            TimeZone.setDefault(original);
        }
    }
}

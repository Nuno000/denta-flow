package ar.dentaflow.aplicacion.disponibilidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Spec horarios-atencion de punta a punta sobre H2: alta, rechazo por superposición, contiguo y orden. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ServicioDeHorariosIntegracionTest {

    @Autowired
    private ServicioDeHorarios servicio;

    @Autowired
    private ProfesionalRepositorio profesionales;

    private static LocalTime hora(String texto) {
        return LocalTime.parse(texto);
    }

    @Test
    void altaSuperposicionContiguoYOrden() {
        Long profesional = profesionales.guardar(
                Profesional.nuevo("Laura", "Integración", null, "laura.integracion@example.com")).getId();

        servicio.crear(profesional, DayOfWeek.TUESDAY, hora("09:00"), hora("13:00"));
        servicio.crear(profesional, DayOfWeek.MONDAY, hora("09:00"), hora("13:00"));
        assertThatThrownBy(() -> servicio.crear(profesional, DayOfWeek.MONDAY, hora("12:00"), hora("16:00")))
                .isInstanceOf(ConflictoException.class);
        servicio.crear(profesional, DayOfWeek.MONDAY, hora("13:00"), hora("17:00"));

        assertThat(servicio.listarDelProfesional(profesional))
                .extracting(HorarioAtencion::getDia, HorarioAtencion::getInicio)
                .containsExactly(
                        tuple(DayOfWeek.MONDAY, hora("09:00")),
                        tuple(DayOfWeek.MONDAY, hora("13:00")),
                        tuple(DayOfWeek.TUESDAY, hora("09:00")));
    }
}

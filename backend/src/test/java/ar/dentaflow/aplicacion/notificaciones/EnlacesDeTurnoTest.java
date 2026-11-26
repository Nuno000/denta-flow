package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Spec enlaces-paciente — "Enlaces armados con la URL pública y el token del turno" (design D6). Usa el token que
 * el turno ya tiene: no hay generador de tokens en juego.
 */
class EnlacesDeTurnoTest {

    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrStUvWxYz0123456789-_abcde";

    private final Turno turno = Turno.nuevo(1L, 2L, 3L, 4L, Instant.parse("2026-12-02T13:00:00Z"), 30,
            OrigenTurno.ONLINE, false, null, Instant.parse("2026-11-30T12:00:00Z"), TOKEN);

    private EnlacesDeTurno enlaces(String base) {
        return new EnlacesDeTurno(new PropiedadesEnlaces(base));
    }

    @Test
    void armaLosTresEnlacesConElTokenDelTurno() {
        EnlacesDeTurno enlaces = enlaces("https://turnos.example");

        assertThat(TOKEN).hasSize(43);
        assertThat(enlaces.confirmar(turno)).isEqualTo("https://turnos.example/turno/" + TOKEN + "/confirmar");
        assertThat(enlaces.cancelar(turno)).isEqualTo("https://turnos.example/turno/" + TOKEN + "/cancelar");
        assertThat(enlaces.reprogramar(turno)).isEqualTo("https://turnos.example/turno/" + TOKEN + "/reprogramar");
    }

    @Test
    void conBaseTerminadaEnBarraNoQuedanBarrasDobles() {
        EnlacesDelTurno todos = enlaces("https://turnos.example/").todos(turno);

        assertThat(todos.confirmar()).isEqualTo("https://turnos.example/turno/" + TOKEN + "/confirmar");
        assertThat(todos.confirmar().substring("https://".length())).doesNotContain("//");
    }

    @Test
    void dosLlamadasDanLosMismosEnlaces() {
        EnlacesDeTurno enlaces = enlaces("https://turnos.example");

        assertThat(enlaces.todos(turno)).isEqualTo(enlaces.todos(turno));
        assertThat(turno.tokenAcceso()).isEqualTo(TOKEN);
    }

    @Test
    void toStringNoMuestraLosEnlaces() {
        assertThat(enlaces("https://turnos.example").todos(turno).toString()).doesNotContain(TOKEN);
    }
}

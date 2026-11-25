package ar.dentaflow.aplicacion.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.HistorialDeTurnoRepositorio;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Spec "Registro de quién y cuándo en cada cambio de turno", sin base ni Spring. */
class ServicioDeAuditoriaTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T13:15:00Z");

    private final HistorialFalso historial = new HistorialFalso();
    private final ServicioDeAuditoria servicio =
            new ServicioDeAuditoria(historial, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    void registraElCambioConLaHoraDelSistema() {
        CambioDeTurno registrado = servicio.registrarCambio(42L, AccionDeTurno.CAMBIO_ESTADO, Actor.usuario(7L),
                Map.of("estado", "pendiente"), Map.of("estado", "confirmado"));

        assertThat(historial.agregados).containsExactly(registrado);
        assertThat(registrado.turnoId()).isEqualTo(42L);
        assertThat(registrado.actor()).isEqualTo(Actor.usuario(7L));
        assertThat(registrado.ocurridoEn()).isEqualTo(AHORA);
        assertThat(registrado.accion()).isEqualTo(AccionDeTurno.CAMBIO_ESTADO);
        assertThat(registrado.valorAnterior()).containsExactly(Map.entry("estado", "pendiente"));
        assertThat(registrado.valorNuevo()).containsExactly(Map.entry("estado", "confirmado"));
    }

    @Test
    void laHoraSaleDelRelojYNoDeQuienLlama() {
        ServicioDeAuditoria otraHora = new ServicioDeAuditoria(historial,
                Clock.fixed(Instant.parse("2026-12-02T09:00:00Z"), ZoneOffset.UTC));

        CambioDeTurno registrado = otraHora.registrarCambio(43L, AccionDeTurno.CREACION, Actor.paciente(), null,
                Map.of("estado", "pendiente"));

        assertThat(registrado.ocurridoEn()).isEqualTo(Instant.parse("2026-12-02T09:00:00Z"));
    }

    @Test
    void unCambioInvalidoNoSeAgrega() {
        assertThatThrownBy(() -> servicio.registrarCambio(42L, AccionDeTurno.CAMBIO_ESTADO, Actor.sistema(),
                null, null)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> servicio.registrarCambio(42L, AccionDeTurno.CANCELACION, null,
                Map.of("estado", "pendiente"), Map.of("estado", "cancelado")))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThat(historial.agregados).isEmpty();
    }

    @Test
    void elHistorialSeConsultaEnElPuerto() {
        CambioDeTurno primero = servicio.registrarCambio(42L, AccionDeTurno.CREACION, Actor.sistema(), null,
                Map.of("estado", "pendiente"));
        servicio.registrarCambio(99L, AccionDeTurno.CREACION, Actor.sistema(), null, Map.of("estado", "pendiente"));

        assertThat(servicio.historialDe(42L)).containsExactly(primero);
        assertThat(servicio.historialDe(7L)).isEmpty();
    }

    private static final class HistorialFalso implements HistorialDeTurnoRepositorio {

        private final List<CambioDeTurno> agregados = new ArrayList<>();

        @Override
        public CambioDeTurno agregar(CambioDeTurno cambio) {
            agregados.add(cambio);
            return cambio;
        }

        @Override
        public List<CambioDeTurno> historialDe(long turnoId) {
            return agregados.stream().filter(c -> c.turnoId() == turnoId).toList();
        }
    }
}

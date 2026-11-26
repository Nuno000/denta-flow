package ar.dentaflow.infraestructura.persistencia.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.notificaciones.EstadoNotificacion;
import ar.dentaflow.dominio.notificaciones.MotivoFallo;
import ar.dentaflow.dominio.notificaciones.Notificacion;
import ar.dentaflow.dominio.notificaciones.NotificacionRepositorio;
import ar.dentaflow.dominio.notificaciones.TipoNotificacion;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Persistencia de notificaciones (design D2 y D3): ida y vuelta y códigos guardados en minúscula. */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificacionRepositorioTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-12-01T12:00:05.123456Z");

    @Autowired
    private NotificacionRepositorio notificaciones;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private long turno;

    @BeforeEach
    void crearTurno() {
        turno = new EscenarioDeAgendaJdbc(jdbc).turnoValido();
    }

    @Test
    void guardaYEncuentraPorTurnoYTipo() {
        Notificacion n = Notificacion.nueva(turno, TipoNotificacion.CONFIRMACION, T0);
        n.registrarIntento(T1);
        n.marcarEnviada(T1);
        Notificacion guardada = notificaciones.guardar(n);
        em.flush();
        em.clear();

        Notificacion leida = notificaciones.buscarPorTurnoYTipo(turno, TipoNotificacion.CONFIRMACION).orElseThrow();
        assertThat(leida.getId()).isEqualTo(guardada.getId());
        assertThat(leida.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(leida.getIntentos()).isEqualTo(1);
        assertThat(leida.getEnviadaEn()).isEqualTo(T1);
        assertThat(notificaciones.buscarPorTurnoYTipo(turno, TipoNotificacion.RECORDATORIO)).isEmpty();
        assertThat(notificaciones.buscarPorId(guardada.getId())).isPresent();
    }

    @Test
    void guardaLosCodigosEnMinuscula() {
        Notificacion n = Notificacion.nueva(turno, TipoNotificacion.CONFIRMACION, T0);
        n.registrarIntento(T1);
        n.marcarFallida(MotivoFallo.CONEXION, T1);
        notificaciones.guardar(n);
        em.flush();

        Map<String, Object> fila = jdbc.queryForMap(
                "SELECT tipo, canal, estado, motivo_fallo FROM notificacion WHERE turno_id = ?", turno);
        assertThat(fila).containsEntry("tipo", "confirmacion").containsEntry("canal", "email")
                .containsEntry("estado", "fallida").containsEntry("motivo_fallo", "conexion");
    }

    @Test
    void actualizaUnaNotificacionExistente() {
        Notificacion n = notificaciones.guardar(Notificacion.nueva(turno, TipoNotificacion.CONFIRMACION, T0));
        em.flush();
        em.clear();

        Notificacion leida = notificaciones.buscarPorId(n.getId()).orElseThrow();
        leida.omitir(T1);
        notificaciones.guardar(leida);
        em.flush();

        assertThat(jdbc.queryForObject("SELECT estado FROM notificacion WHERE id = ?", String.class, n.getId()))
                .isEqualTo("omitida");
    }
}

package ar.dentaflow.aplicacion.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Spec "Registro atómico con el cambio auditado" contra la base H2 real. Sin {@code @Transactional} en la
 * clase: cada caso abre (o no) su propia transacción para probar confirmación y reversión de verdad.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicioDeAuditoriaIntegracionTest {

    private static final Map<String, String> PENDIENTE = Map.of("estado", "pendiente");
    private static final Map<String, String> CONFIRMADO = Map.of("estado", "confirmado");

    @Autowired
    private ServicioDeAuditoria auditoria;

    @Autowired
    private TransactionTemplate transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    /** Un turno real propio por caso (FK desde C-09): no depende de que la tabla esté vacía. */
    private long turno;
    private EscenarioDeAgendaJdbc escenario;

    @BeforeEach
    void crearTurno() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        turno = escenario.turnoValido();
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    private long registros() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id = ?", Long.class, turno);
    }

    @Test
    void sinTransaccionSeRechazaYNoSeGuardaNada() {
        assertThatThrownBy(() -> auditoria.registrarCambio(turno, AccionDeTurno.CONFIRMACION, Actor.sistema(),
                PENDIENTE, CONFIRMADO)).isInstanceOf(IllegalTransactionStateException.class);

        assertThat(registros()).isZero();
    }

    @Test
    void siLaOperacionTerminaBienElRegistroQueda() {
        transaccion.executeWithoutResult(estado -> auditoria.registrarCambio(turno, AccionDeTurno.CONFIRMACION,
                Actor.sistema(), PENDIENTE, CONFIRMADO));

        assertThat(registros()).isEqualTo(1L);
        assertThat(auditoria.historialDe(turno)).singleElement()
                .satisfies(cambio -> assertThat(cambio.accion()).isEqualTo(AccionDeTurno.CONFIRMACION));
    }

    @Test
    void siLaOperacionFallaDespuesDeRegistrarNoQuedaNada() {
        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            auditoria.registrarCambio(turno, AccionDeTurno.CONFIRMACION, Actor.sistema(), PENDIENTE, CONFIRMADO);
            throw new IllegalStateException("Solapamiento detectado");
        })).isInstanceOf(IllegalStateException.class).hasMessage("Solapamiento detectado");

        assertThat(registros()).isZero();
    }

    @Test
    void unUsuarioInexistenteFallaEnElRegistroYNoAlConfirmar() {
        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            try {
                auditoria.registrarCambio(turno, AccionDeTurno.CONFIRMACION, Actor.usuario(999_999_999L),
                        PENDIENTE, CONFIRMADO);
            } catch (DataIntegrityViolationException esperado) {
                estado.setRollbackOnly();
                throw new IllegalStateException("falló en registrarCambio", esperado);
            }
        })).isInstanceOf(IllegalStateException.class).hasMessage("falló en registrarCambio");

        assertThat(registros()).isZero();
    }

    @Test
    void valoresDemasiadoLargosSeRechazanComoValidacionYRevierteTodo() {
        Map<String, String> enorme = Map.of("motivo", "x".repeat(5000));

        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            auditoria.registrarCambio(turno, AccionDeTurno.CREACION, Actor.sistema(), null, CONFIRMADO);
            auditoria.registrarCambio(turno, AccionDeTurno.CAMBIO_ESTADO, Actor.sistema(), PENDIENTE, enorme);
        })).isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("4000");

        assertThat(registros()).isZero();
    }

    @Test
    void registrarUnCambioDeUnTurnoInexistenteRevierteTodo() {
        long inexistente = 9999L;
        jdbc.update("DELETE FROM turno_historial WHERE turno_id = ?", inexistente);

        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            auditoria.registrarCambio(turno, AccionDeTurno.CREACION, Actor.sistema(), null, CONFIRMADO);
            auditoria.registrarCambio(inexistente, AccionDeTurno.CREACION, Actor.sistema(), null, CONFIRMADO);
        })).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(registros()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id = ?", Long.class,
                inexistente)).isZero();
    }
}

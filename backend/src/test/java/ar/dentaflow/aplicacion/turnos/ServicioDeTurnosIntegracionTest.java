package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.auditoria.ServicioDeAuditoria;
import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.TipoDeActor;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.RecursoOcupado;
import ar.dentaflow.dominio.turnos.TransicionDeEstadoInvalidaException;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Specs turnos y auditoria-turnos contra H2: atomicidad con la auditoría, cancelación que libera el horario,
 * sobreturno, estados y reprogramación, y la guarda de aislamiento (design D5). Sin {@code @Transactional} en la
 * clase: cada llamada al servicio abre y confirma su propia transacción. El reloj es fijo para no depender del
 * día en que se corre.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicioDeTurnosIntegracionTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");
    private static final Instant DOCE = Instant.parse("2026-12-01T12:00:00Z");
    private static final Actor PACIENTE = Actor.paciente();

    @TestConfiguration
    static class RelojFijo {

        @Bean
        @Primary
        Clock relojFijoDePrueba() {
            return Clock.fixed(AHORA, ZoneOffset.UTC);
        }
    }

    @Autowired
    private ServicioDeTurnos servicio;

    @Autowired
    private ServicioDeAuditoria auditoria;

    @Autowired
    private TransactionTemplate transaccion;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profesional;
    private long sillon;
    private long prestacion;
    private long usuario;
    private Actor recepcion;

    @BeforeEach
    void crearEscenario() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(profesional, prestacion);
        usuario = escenario.usuario();
        recepcion = Actor.usuario(usuario);
    }

    @AfterEach
    void limpiar() {
        escenario.limpiar();
    }

    private SolicitudDeTurno online(long profesionalId, long sillonId, Instant inicio) {
        return new SolicitudDeTurno(paciente, profesionalId, sillonId, prestacion, inicio, OrigenTurno.ONLINE, false,
                PACIENTE);
    }

    private SolicitudDeTurno deRecepcion(Instant inicio, boolean sobreturno) {
        return new SolicitudDeTurno(paciente, profesional, sillon, prestacion, inicio, OrigenTurno.RECEPCION,
                sobreturno, recepcion);
    }

    private long turnosDelProfesional() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE profesional_id = ?", Long.class, profesional);
    }

    private long historialDelProfesional() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id IN "
                + "(SELECT id FROM turno WHERE profesional_id = ?)", Long.class, profesional);
    }

    private long idDe(TurnoCreado creado) {
        return creado.turno().getId();
    }

    // --- Creación ---

    @Test
    void laAltaDejaUnTurnoPendienteYUnRegistroDeCreacionSinDatosPersonalesNiToken() {
        TurnoCreado creado = servicio.crear(online(profesional, sillon, DOCE));

        long id = idDe(creado);
        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, id))
                .isEqualTo("pendiente");
        List<CambioDeTurno> historial = auditoria.historialDe(id);
        assertThat(historial).singleElement().satisfies(cambio -> {
            assertThat(cambio.accion()).isEqualTo(AccionDeTurno.CREACION);
            assertThat(cambio.actor().tipo()).isEqualTo(TipoDeActor.PACIENTE);
            assertThat(cambio.valorNuevo()).containsKeys("paciente_id", "es_sobreturno");
        });
        String json = jdbc.queryForObject("SELECT valor_nuevo FROM turno_historial WHERE turno_id = ?", String.class,
                id);
        String dni = jdbc.queryForObject("SELECT dni FROM paciente WHERE id = ?", String.class, paciente);
        assertThat(json).doesNotContain(creado.tokenAcceso()).doesNotContain(dni).doesNotContain("@")
                .doesNotContain("1155550000").doesNotContain("Ana").doesNotContain("Ejemplo");
    }

    @Test
    void unConflictoNoDejaOtroTurnoNiHistorial() {
        servicio.crear(online(profesional, sillon, DOCE));
        long otroSillon = escenario.sillon();

        assertThatThrownBy(() -> servicio.crear(online(profesional, otroSillon, DOCE.plusSeconds(15 * 60))))
                .isInstanceOfSatisfying(TurnoSolapadoException.class,
                        e -> assertThat(e.recurso()).isEqualTo(RecursoOcupado.PROFESIONAL));

        assertThat(turnosDelProfesional()).isEqualTo(1L);
        assertThat(historialDelProfesional()).isEqualTo(1L);
    }

    @Test
    void elConflictoDeSillonSeDetectaConOtroProfesional() {
        servicio.crear(online(profesional, sillon, DOCE));
        long otroProfesional = escenario.profesional();
        escenario.habilitar(otroProfesional, prestacion);

        assertThatThrownBy(() -> servicio.crear(online(otroProfesional, sillon, DOCE)))
                .isInstanceOfSatisfying(TurnoSolapadoException.class,
                        e -> assertThat(e.recurso()).isEqualTo(RecursoOcupado.SILLON));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE sillon_id = ?", Long.class, sillon))
                .isEqualTo(1L);
    }

    @Test
    void unTurnoContiguoSeAcepta() {
        servicio.crear(online(profesional, sillon, DOCE));

        servicio.crear(online(profesional, sillon, DOCE.plusSeconds(30 * 60)));

        assertThat(turnosDelProfesional()).isEqualTo(2L);
    }

    @Test
    void unTurnoCanceladoPorSqlLiberaElRango() {
        TurnoCreado primero = servicio.crear(online(profesional, sillon, DOCE));
        jdbc.update("UPDATE turno SET estado = 'cancelado' WHERE id = ?", idDe(primero));

        servicio.crear(online(profesional, sillon, DOCE));

        assertThat(turnosDelProfesional()).isEqualTo(2L);
    }

    @Test
    void unSobreturnoDeRecepcionSuperpuestoSeCreaYNoImpideUnTurnoComun() {
        servicio.crear(online(profesional, sillon, DOCE));

        TurnoCreado sobreturno = servicio.crear(deRecepcion(DOCE, true));
        assertThat(sobreturno.turno().isEsSobreturno()).isTrue();
        assertThat(auditoria.historialDe(idDe(sobreturno)).getFirst().valorNuevo())
                .containsEntry("es_sobreturno", "true");

        // El sobreturno no ocupa el horario: un turno común en otro lugar libre sigue entrando, y en el mismo
        // horario solo choca con el turno común original.
        long otroProfesional = escenario.profesional();
        long otroSillon = escenario.sillon();
        escenario.habilitar(otroProfesional, prestacion);
        servicio.crear(online(otroProfesional, otroSillon, DOCE));
    }

    @Test
    void siSoloHaySobreturnoUnTurnoComunEnElMismoHorarioSeAcepta() {
        servicio.crear(deRecepcion(DOCE, true));

        TurnoCreado comun = servicio.crear(online(profesional, sillon, DOCE));

        assertThat(comun.turno().isEsSobreturno()).isFalse();
        assertThat(turnosDelProfesional()).isEqualTo(2L);
    }

    @Test
    void llamarloDentroDeUnaTransaccionConAislamientoPorDefectoFallaConIllegalState() {
        assertThatThrownBy(() -> transaccion.executeWithoutResult(
                estado -> servicio.crear(online(profesional, sillon, DOCE))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("READ COMMITTED");

        assertThat(turnosDelProfesional()).isZero();
    }

    // --- Estados ---

    @Test
    void laCancelacionPorElPacienteDejaCanceladoYUnRegistroConActorPaciente() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));

        Turno cancelado = servicio.cancelar(id, PACIENTE);

        assertThat(cancelado.getEstado()).isEqualTo(EstadoTurno.CANCELADO);
        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, id))
                .isEqualTo("cancelado");
        CambioDeTurno ultimo = auditoria.historialDe(id).getLast();
        assertThat(ultimo.accion()).isEqualTo(AccionDeTurno.CANCELACION);
        assertThat(ultimo.actor()).isEqualTo(PACIENTE);
        assertThat(ultimo.valorAnterior()).containsEntry("estado", "pendiente");
        assertThat(ultimo.valorNuevo()).containsEntry("estado", "cancelado");
    }

    @Test
    void laDobleConfirmacionNoSumaRegistros() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));

        servicio.confirmar(id, recepcion);
        servicio.confirmar(id, recepcion);

        assertThat(auditoria.historialDe(id)).extracting(CambioDeTurno::accion)
                .containsExactly(AccionDeTurno.CREACION, AccionDeTurno.CONFIRMACION);
    }

    @Test
    void confirmarUnCanceladoEsConflictoYNoCambiaNada() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));
        servicio.cancelar(id, PACIENTE);

        assertThatThrownBy(() -> servicio.confirmar(id, recepcion))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);

        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, id))
                .isEqualTo("cancelado");
        assertThat(auditoria.historialDe(id)).hasSize(2);
    }

    @Test
    void despuesDeCancelarOtroTurnoTomaElMismoRango() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));
        servicio.cancelar(id, PACIENTE);

        TurnoCreado otro = servicio.crear(online(profesional, sillon, DOCE));

        assertThat(otro.turno().getEstado()).isEqualTo(EstadoTurno.PENDIENTE);
    }

    @Test
    void laAsistenciaSeAuditaComoCambioDeEstado() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));
        servicio.confirmar(id, recepcion);

        servicio.cambiarEstado(id, EstadoTurno.ASISTIO, recepcion);
        servicio.cambiarEstado(id, EstadoTurno.AUSENTE, recepcion);

        assertThat(auditoria.historialDe(id)).extracting(CambioDeTurno::accion).containsExactly(
                AccionDeTurno.CREACION, AccionDeTurno.CONFIRMACION, AccionDeTurno.CAMBIO_ESTADO,
                AccionDeTurno.CAMBIO_ESTADO);
    }

    // --- Reprogramación ---

    @Test
    void correrQuinceMinutosUnTurnoNoChocaConsigoMismo() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));

        Turno movido = servicio.reprogramar(id,
                new SolicitudDeReprogramacion(DOCE.plusSeconds(15 * 60), profesional, sillon), recepcion);

        assertThat(movido.getInicio()).isEqualTo(Instant.parse("2026-12-01T12:15:00Z"));
        assertThat(movido.getFin()).isEqualTo(Instant.parse("2026-12-01T12:45:00Z"));
        assertThat(turnosDelProfesional()).isEqualTo(1L);
    }

    @Test
    void reprogramarDejaElHistorialConInicioYFinAnterioresYNuevosEnUtc() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));

        servicio.reprogramar(id,
                new SolicitudDeReprogramacion(Instant.parse("2026-12-02T15:30:00Z"), profesional, sillon), recepcion);

        CambioDeTurno cambio = auditoria.historialDe(id).getLast();
        assertThat(cambio.accion()).isEqualTo(AccionDeTurno.REPROGRAMACION);
        assertThat(cambio.actor()).isEqualTo(recepcion);
        assertThat(cambio.valorAnterior()).isEqualTo(Map.of("inicio", "2026-12-01T12:00:00Z",
                "fin", "2026-12-01T12:30:00Z"));
        assertThat(cambio.valorNuevo()).isEqualTo(Map.of("inicio", "2026-12-02T15:30:00Z",
                "fin", "2026-12-02T16:00:00Z"));
    }

    @Test
    void unDestinoOcupadoDejaElTurnoComoEstaba() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));
        servicio.crear(online(profesional, sillon, DOCE.plusSeconds(60 * 60)));

        assertThatThrownBy(() -> servicio.reprogramar(id,
                new SolicitudDeReprogramacion(DOCE.plusSeconds(45 * 60), profesional, sillon), recepcion))
                .isInstanceOf(TurnoSolapadoException.class);

        assertThat(jdbc.queryForObject("SELECT inicio FROM turno WHERE id = ?", java.sql.Timestamp.class, id)
                .toInstant()).isEqualTo(DOCE);
        assertThat(auditoria.historialDe(id)).hasSize(1);
    }

    @Test
    void reprogramarUnCanceladoEsConflicto() {
        long id = idDe(servicio.crear(online(profesional, sillon, DOCE)));
        servicio.cancelar(id, PACIENTE);

        assertThatThrownBy(() -> servicio.reprogramar(id,
                new SolicitudDeReprogramacion(DOCE.plusSeconds(3600), profesional, sillon), recepcion))
                .isInstanceOf(TransicionDeEstadoInvalidaException.class);
    }
}

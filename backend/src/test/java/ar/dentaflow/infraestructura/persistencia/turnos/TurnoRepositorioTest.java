package ar.dentaflow.infraestructura.persistencia.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OcupacionDeRecurso;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec turnos — persistencia, "Sin solapamiento por profesional ni por sillón", "Cancelación libera el horario",
 * "Solapamiento evaluado sobre instantes absolutos" y "Token de acceso" (design D5, D9, D10).
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TurnoRepositorioTest {

    private static final Instant DOCE = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant DOCE_Y_MEDIA = Instant.parse("2026-12-01T12:30:00Z");
    private static final Instant UNA = Instant.parse("2026-12-01T13:00:00Z");

    @Autowired
    private TurnoRepositorio turnos;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profesional;
    private long sillon;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void crearRecursos() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        usuario = escenario.usuario();
    }

    private Turno guardar(long profesionalId, long sillonId, Instant inicio, int minutos, EstadoTurno estado,
            boolean sobreturno) {
        Turno turno = Turno.nuevo(paciente, profesionalId, sillonId, prestacion, inicio, minutos,
                OrigenTurno.RECEPCION, sobreturno, usuario, Instant.parse("2026-11-30T12:00:00Z"),
                escenario.tokenNuevo());
        if (estado != EstadoTurno.PENDIENTE) {
            turno.cambiarEstado(estado);
        }
        Turno guardado = turnos.guardar(turno);
        em.flush();
        em.clear();
        return guardado;
    }

    private Turno guardarActivo(Instant inicio, int minutos) {
        return guardar(profesional, sillon, inicio, minutos, EstadoTurno.PENDIENTE, false);
    }

    // --- Mapeo ---

    @Test
    void guardaYRecuperaTodoConInstantesExactosEnUtcYEstadoYOrigenPorCodigo() {
        Instant inicio = Instant.parse("2026-12-01T15:45:00Z");
        Turno turno = Turno.nuevo(paciente, profesional, sillon, prestacion, inicio, 45, OrigenTurno.ONLINE, false,
                null, Instant.parse("2026-11-30T12:00:00.123456Z"), "a".repeat(43));
        turno.cambiarEstado(EstadoTurno.CONFIRMADO);

        Long id = turnos.guardar(turno).getId();
        em.flush();
        em.clear();

        Turno leido = turnos.buscarPorId(id).orElseThrow();
        assertThat(leido.getPacienteId()).isEqualTo(paciente);
        assertThat(leido.getProfesionalId()).isEqualTo(profesional);
        assertThat(leido.getSillonId()).isEqualTo(sillon);
        assertThat(leido.getPrestacionId()).isEqualTo(prestacion);
        assertThat(leido.getInicio()).isEqualTo(inicio);
        assertThat(leido.getFin()).isEqualTo(Instant.parse("2026-12-01T16:30:00Z"));
        assertThat(leido.getEstado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(leido.getOrigen()).isEqualTo(OrigenTurno.ONLINE);
        assertThat(leido.isEsSobreturno()).isFalse();
        assertThat(leido.getCreadoPorUsuarioId()).isNull();
        assertThat(leido.getCreadoEn()).isEqualTo(Instant.parse("2026-11-30T12:00:00.123456Z"));
        assertThat(leido.tokenAcceso()).isEqualTo("a".repeat(43));
        assertThat(jdbc.queryForObject("SELECT estado FROM turno WHERE id = ?", String.class, id))
                .isEqualTo("confirmado");
        assertThat(jdbc.queryForObject("SELECT origen FROM turno WHERE id = ?", String.class, id))
                .isEqualTo("online");
    }

    @Test
    void guardaYRecuperaUnSobreturnoDeRecepcionConSuCreador() {
        Long id = guardar(profesional, sillon, DOCE, 30, EstadoTurno.PENDIENTE, true).getId();

        Turno leido = turnos.buscarPorId(id).orElseThrow();

        assertThat(leido.isEsSobreturno()).isTrue();
        assertThat(leido.getCreadoPorUsuarioId()).isEqualTo(usuario);
    }

    @Test
    void buscarPorIdInexistenteDevuelveVacio() {
        assertThat(turnos.buscarPorId(999_999_999L)).isEmpty();
    }

    // --- Token ---

    @Test
    void buscarPorTokenEncuentraElTurno() {
        Turno turno = guardarActivo(DOCE, 30);

        assertThat(turnos.buscarPorToken(turno.tokenAcceso())).map(Turno::getId).contains(turno.getId());
    }

    @Test
    void buscarPorTokenInexistenteOMalFormadoDevuelveVacio() {
        assertThat(turnos.buscarPorToken(escenario.tokenNuevo())).isEmpty();
        assertThat(turnos.buscarPorToken("corto")).isEmpty();
        assertThat(turnos.buscarPorToken("x".repeat(43) + "'; DROP TABLE turno;--")).isEmpty();
        assertThat(turnos.buscarPorToken("")).isEmpty();
        assertThat(turnos.buscarPorToken(null)).isEmpty();
    }

    // --- Solapamiento ---

    @Test
    void unaSuperposicionParcialOTotalEsSolapamientoDeProfesionalYDeSillon() {
        guardarActivo(DOCE, 30);

        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T12:15:00Z"),
                Instant.parse("2026-12-01T12:45:00Z"), null)).isTrue();
        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T11:45:00Z"),
                Instant.parse("2026-12-01T12:15:00Z"), null)).isTrue();
        assertThat(turnos.haySolapamientoDeProfesional(profesional, DOCE, DOCE_Y_MEDIA, null)).isTrue();
        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T11:00:00Z"), UNA,
                null)).isTrue();
        assertThat(turnos.haySolapamientoDeSillon(sillon, Instant.parse("2026-12-01T12:15:00Z"),
                Instant.parse("2026-12-01T12:45:00Z"), null)).isTrue();
        assertThat(turnos.haySolapamientoDeSillon(sillon, DOCE, DOCE_Y_MEDIA, null)).isTrue();
    }

    @Test
    void losTurnosContiguosNoSeSolapan() {
        guardarActivo(DOCE, 30);

        assertThat(turnos.haySolapamientoDeProfesional(profesional, DOCE_Y_MEDIA, UNA, null)).isFalse();
        assertThat(turnos.haySolapamientoDeSillon(sillon, DOCE_Y_MEDIA, UNA, null)).isFalse();
        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T11:30:00Z"), DOCE,
                null)).isFalse();
        assertThat(turnos.haySolapamientoDeSillon(sillon, Instant.parse("2026-12-01T11:30:00Z"), DOCE, null))
                .isFalse();
    }

    @Test
    void unTurnoCanceladoNoOcupaElHorario() {
        guardar(profesional, sillon, DOCE, 30, EstadoTurno.CANCELADO, false);

        assertThat(turnos.haySolapamientoDeProfesional(profesional, DOCE, DOCE_Y_MEDIA, null)).isFalse();
        assertThat(turnos.haySolapamientoDeSillon(sillon, DOCE, DOCE_Y_MEDIA, null)).isFalse();
    }

    @Test
    void cualquierEstadoQueNoSeaCanceladoOcupaElHorario() {
        for (EstadoTurno estado : EstadoTurno.values()) {
            if (estado == EstadoTurno.CANCELADO) {
                continue;
            }
            long otroProfesional = escenario.profesional();
            long otroSillon = escenario.sillon();
            guardar(otroProfesional, otroSillon, DOCE, 30, estado, false);

            assertThat(turnos.haySolapamientoDeProfesional(otroProfesional, DOCE, DOCE_Y_MEDIA, null))
                    .as(estado.codigo()).isTrue();
            assertThat(turnos.haySolapamientoDeSillon(otroSillon, DOCE, DOCE_Y_MEDIA, null))
                    .as(estado.codigo()).isTrue();
        }
    }

    @Test
    void unSobreturnoExistenteNoOcupaElHorarioParaOtrosTurnos() {
        guardar(profesional, sillon, DOCE, 30, EstadoTurno.PENDIENTE, true);

        assertThat(turnos.haySolapamientoDeProfesional(profesional, DOCE, DOCE_Y_MEDIA, null)).isFalse();
        assertThat(turnos.haySolapamientoDeSillon(sillon, DOCE, DOCE_Y_MEDIA, null)).isFalse();
    }

    @Test
    void excluirElPropioTurnoPermiteMoverloSobreSiMismo() {
        Turno propio = guardarActivo(DOCE, 30);

        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T12:15:00Z"),
                Instant.parse("2026-12-01T12:45:00Z"), propio.getId())).isFalse();
        assertThat(turnos.haySolapamientoDeSillon(sillon, Instant.parse("2026-12-01T12:15:00Z"),
                Instant.parse("2026-12-01T12:45:00Z"), propio.getId())).isFalse();
    }

    @Test
    void excluirUnTurnoNoIgnoraAOtroQueSiSolapa() {
        Turno propio = guardarActivo(DOCE, 30);
        Turno otro = guardar(profesional, sillon, DOCE_Y_MEDIA, 30, EstadoTurno.PENDIENTE, false);

        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T12:15:00Z"),
                Instant.parse("2026-12-01T12:45:00Z"), propio.getId())).isTrue();
        assertThat(turnos.haySolapamientoDeProfesional(profesional, Instant.parse("2026-12-01T12:15:00Z"),
                Instant.parse("2026-12-01T12:45:00Z"), otro.getId())).isTrue();
    }

    @Test
    void otroProfesionalOtroSillonNoSeSolapan() {
        guardarActivo(DOCE, 30);

        assertThat(turnos.haySolapamientoDeProfesional(escenario.profesional(), DOCE, DOCE_Y_MEDIA, null)).isFalse();
        assertThat(turnos.haySolapamientoDeSillon(escenario.sillon(), DOCE, DOCE_Y_MEDIA, null)).isFalse();
    }

    @Test
    void elSolapamientoSeEvaluaSobreInstantesAbsolutosAunqueCrucenLaMedianocheLocal() {
        Instant desde = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(2026, 12, 1, 23, 45));
        guardarActivo(desde, 30);
        Instant medianoche = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(2026, 12, 2, 0, 0));

        assertThat(turnos.haySolapamientoDeProfesional(profesional, medianoche, medianoche.plusSeconds(30 * 60),
                null)).isTrue();
        assertThat(turnos.haySolapamientoDeProfesional(profesional, medianoche.plusSeconds(15 * 60),
                medianoche.plusSeconds(45 * 60), null)).isFalse();
    }

    @Test
    void buscarPorIdParaActualizarDevuelveElTurno() {
        Turno turno = guardarActivo(DOCE, 30);

        assertThat(turnos.buscarPorIdParaActualizar(turno.getId())).map(Turno::getId).contains(turno.getId());
        assertThat(turnos.buscarPorIdParaActualizar(999_999_999L)).isEmpty();
    }

    @Test
    void listarOcupacionesDeProfesionalesYSillonesFiltraPorEstadoSobreturnoBordeYExclusion() {
        long profB = escenario.profesional();
        long profC = escenario.profesional();
        long sillonB = escenario.sillon();
        long sillonC = escenario.sillon();

        Turno pendienteA = guardar(profesional, sillon, DOCE, 30, EstadoTurno.PENDIENTE, false);
        guardar(profB, sillonB, DOCE_Y_MEDIA, 30, EstadoTurno.CONFIRMADO, false);
        guardar(profesional, sillon, UNA, 30, EstadoTurno.ASISTIO, false);
        guardar(profesional, sillon, DOCE, 30, EstadoTurno.CANCELADO, false);
        guardar(profesional, sillon, DOCE, 30, EstadoTurno.PENDIENTE, true);
        guardar(profC, sillonC, DOCE, 30, EstadoTurno.PENDIENTE, false);
        guardar(profesional, sillon, Instant.parse("2026-12-01T11:30:00Z"), 30, EstadoTurno.PENDIENTE, false);

        Instant desde = DOCE;
        Instant hasta = Instant.parse("2026-12-01T13:30:00Z");

        assertThat(turnos.listarOcupacionesDeProfesionales(
                java.util.List.of(profesional, profB), desde, hasta, null))
                .containsExactlyInAnyOrder(
                        new OcupacionDeRecurso(profesional, DOCE, DOCE_Y_MEDIA),
                        new OcupacionDeRecurso(profB, DOCE_Y_MEDIA, UNA),
                        new OcupacionDeRecurso(profesional, UNA, Instant.parse("2026-12-01T13:30:00Z")));

        assertThat(turnos.listarOcupacionesDeProfesionales(
                java.util.List.of(profesional, profB), desde, hasta, pendienteA.getId()))
                .hasSize(2);

        assertThat(turnos.listarOcupacionesDeSillones(
                java.util.List.of(sillon, sillonB), desde, hasta, pendienteA.getId()))
                .containsExactlyInAnyOrder(
                        new OcupacionDeRecurso(sillonB, DOCE_Y_MEDIA, UNA),
                        new OcupacionDeRecurso(sillon, UNA, Instant.parse("2026-12-01T13:30:00Z")));
    }

    @Test
    void ocupacionesCoincidenExactamenteConHaySolapamientoEnRangosAntesSolapadoYContiguo() {
        guardarActivo(DOCE, 30);

        Instant[][] rangos = {
            { Instant.parse("2026-12-01T11:00:00Z"), DOCE },
            { Instant.parse("2026-12-01T12:15:00Z"), Instant.parse("2026-12-01T12:45:00Z") },
            { DOCE_Y_MEDIA, UNA }
        };

        for (Instant[] r : rangos) {
            boolean solapaProf = turnos.haySolapamientoDeProfesional(profesional, r[0], r[1], null);
            boolean hayOcupProf = !turnos.listarOcupacionesDeProfesionales(
                    java.util.List.of(profesional), r[0], r[1], null).isEmpty();
            assertThat(hayOcupProf).isEqualTo(solapaProf);

            boolean solapaSillon = turnos.haySolapamientoDeSillon(sillon, r[0], r[1], null);
            boolean hayOcupSillon = !turnos.listarOcupacionesDeSillones(
                    java.util.List.of(sillon), r[0], r[1], null).isEmpty();
            assertThat(hayOcupSillon).isEqualTo(solapaSillon);
        }
    }
}

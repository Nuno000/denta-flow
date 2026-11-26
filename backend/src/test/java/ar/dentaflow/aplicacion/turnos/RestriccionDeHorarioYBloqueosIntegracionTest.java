package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.auditoria.ServicioDeAuditoria;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
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

/**
 * Spec turnos — "Turno dentro del horario de atención y fuera de bloqueos" (RN-TU-04, design D5,
 * riesgo R3: verificación dentro de la transacción READ_COMMITTED de ServicioDeTurnos).
 */
@SpringBootTest
@ActiveProfiles("test")
class RestriccionDeHorarioYBloqueosIntegracionTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 30);
    private static final Instant AHORA = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(LUNES, LocalTime.of(8, 0)));

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
    private TurnoRepositorio turnos;

    @Autowired
    private ServicioDeAuditoria auditoria;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profesional;
    private long sillon;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void crearEscenario() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profesional = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(profesional, prestacion);
        usuario = escenario.usuario();
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 1, '09:00:00', '13:00:00')", profesional);
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM bloqueo WHERE modificado_por = ?", usuario);
        escenario.limpiar();
    }

    private static Instant hora(int h, int m) {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(LUNES, LocalTime.of(h, m)));
    }

    private SolicitudDeTurno online(Instant inicio) {
        return new SolicitudDeTurno(paciente, profesional, sillon, prestacion, inicio, OrigenTurno.ONLINE, false,
                Actor.paciente());
    }

    private long cantidadTurnos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE profesional_id = ?", Long.class, profesional);
    }

    private long cantidadHistorial() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM turno_historial WHERE turno_id IN "
                + "(SELECT id FROM turno WHERE profesional_id = ?)", Long.class, profesional);
    }

    @Test
    void rechazaTurnoQueExcedeElHorarioSinDejarTurnoNiHistorial() {
        assertThatThrownBy(() -> servicio.crear(online(hora(12, 45))))
                .isInstanceOf(AgendaNoDisponibleException.class);

        assertThat(cantidadTurnos()).isZero();
        assertThat(cantidadHistorial()).isZero();
    }

    @Test
    void rechazaTurnoEnBloqueoDeProfesional() {
        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, modificado_en) "
                + "VALUES ('PROFESIONAL', ?, NULL, ?, ?, 'AUSENCIA', ?, ?)",
                profesional, Timestamp.from(hora(11, 0)), Timestamp.from(hora(12, 0)), usuario, Timestamp.from(AHORA));

        assertThatThrownBy(() -> servicio.crear(online(hora(11, 30))))
                .isInstanceOf(AgendaNoDisponibleException.class);
        assertThat(cantidadTurnos()).isZero();
    }

    @Test
    void rechazaTurnoEnBloqueoDeSillon() {
        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, modificado_en) "
                + "VALUES ('SILLON', NULL, ?, ?, ?, 'OTRO', ?, ?)",
                sillon, Timestamp.from(hora(10, 0)), Timestamp.from(hora(11, 0)), usuario, Timestamp.from(AHORA));

        assertThatThrownBy(() -> servicio.crear(online(hora(10, 0))))
                .isInstanceOf(AgendaNoDisponibleException.class);
        assertThat(cantidadTurnos()).isZero();
    }

    @Test
    void creaTurnoValidoEnBordeExactoDeHorarioYPasaLaGuardaDeAislamiento() {
        TurnoCreado creado = servicio.crear(online(hora(12, 30)));

        assertThat(creado.turno().getInicio()).isEqualTo(hora(12, 30));
        assertThat(creado.turno().getFin()).isEqualTo(hora(13, 0));
        assertThat(auditoria.historialDe(creado.turno().getId())).hasSize(1);
    }

    @Test
    void rechazaSobreturnoFueraDeHorario() {
        SolicitudDeTurno sobreturno = new SolicitudDeTurno(paciente, profesional, sillon, prestacion,
                hora(13, 0), OrigenTurno.RECEPCION, true, Actor.usuario(usuario));

        assertThatThrownBy(() -> servicio.crear(sobreturno))
                .isInstanceOf(AgendaNoDisponibleException.class);
        assertThat(cantidadTurnos()).isZero();
    }

    @Test
    void rechazaReprogramarAUnBloqueoDeConsultorioYDejaTurnoSinCambios() {
        TurnoCreado creado = servicio.crear(online(hora(9, 0)));
        long turnoId = creado.turno().getId();
        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, modificado_en) "
                + "VALUES ('CONSULTORIO', NULL, NULL, ?, ?, 'FERIADO', ?, ?)",
                Timestamp.from(hora(10, 0)), Timestamp.from(hora(12, 0)), usuario, Timestamp.from(AHORA));

        assertThatThrownBy(() -> servicio.reprogramar(turnoId,
                new SolicitudDeReprogramacion(hora(10, 30), profesional, sillon), Actor.usuario(usuario)))
                .isInstanceOf(AgendaNoDisponibleException.class);

        Turno sinCambios = turnos.buscarPorId(turnoId).orElseThrow();
        assertThat(sinCambios.getInicio()).isEqualTo(hora(9, 0));
        assertThat(auditoria.historialDe(turnoId)).hasSize(1);
    }

    @Test
    void bloqueoCreadoDespuesDeUnTurnoNoLoModifica() {
        TurnoCreado creado = servicio.crear(online(hora(9, 30)));
        long turnoId = creado.turno().getId();

        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, modificado_en) "
                + "VALUES ('PROFESIONAL', ?, NULL, ?, ?, 'AUSENCIA', ?, ?)",
                profesional, Timestamp.from(hora(9, 0)), Timestamp.from(hora(10, 0)), usuario, Timestamp.from(AHORA));

        Turno persistido = turnos.buscarPorId(turnoId).orElseThrow();
        assertThat(persistido.getInicio()).isEqualTo(hora(9, 30));
        assertThat(persistido.getEstado().codigo()).isEqualTo("pendiente");
    }
}

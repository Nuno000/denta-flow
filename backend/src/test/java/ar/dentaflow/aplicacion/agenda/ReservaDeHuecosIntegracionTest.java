package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.HuecoNoDisponibleException;
import ar.dentaflow.dominio.turnos.OrigenTurno;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Integración de {@link ServicioDeReservaDeHuecos} sin transacción envolvente (tarea 5.2, design D6).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReservaDeHuecosIntegracionTest {

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
    private ServicioDeReservaDeHuecos reserva;

    @Autowired
    private ServicioDeTurnos servicioDeTurnos;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profA;
    private long profB;
    private long sillon1;
    private long sillon2;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void crearEscenario() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        profA = escenario.profesional();
        profB = escenario.profesional();
        sillon1 = escenario.sillon();
        sillon2 = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(profA, prestacion);
        escenario.habilitar(profB, prestacion);
        escenario.atiendeTodaLaSemana(profA);
        escenario.atiendeTodaLaSemana(profB);
        usuario = escenario.usuario();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM bloqueo WHERE modificado_por = ?", usuario);
        escenario.limpiar();
    }

    private static Instant hora(int h, int m) {
        return ZonaHorariaConsultorio.aInstante(LocalDateTime.of(LUNES, LocalTime.of(h, m)));
    }

    @Test
    void noEstaAnotadoConTransactional() throws NoSuchMethodException {
        assertThat(ServicioDeReservaDeHuecos.class.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(ServicioDeReservaDeHuecos.class
                .getMethod("reservar", SolicitudDeReservaDeHueco.class)
                .isAnnotationPresent(Transactional.class)).isFalse();
    }

    @Test
    void cualquieraCreaConSegundoProfesionalCuandoElPrimeroFueTomado() {
        servicioDeTurnos.crear(new SolicitudDeTurno(
                paciente, profA, sillon1, prestacion, hora(10, 0), OrigenTurno.ONLINE, false, Actor.paciente()));

        TurnoCreado creado = reserva.reservar(new SolicitudDeReservaDeHueco(
                paciente, prestacion, null, hora(10, 0), OrigenTurno.ONLINE, Actor.paciente()));

        assertThat(creado.turno().getProfesionalId()).isEqualTo(profB);
        assertThat(creado.turno().getSillonId()).isEqualTo(sillon2);
    }

    @Test
    void huecoDeProfesionalUnicoOcupadoOInicioEnBloqueoLanzaConflictoSinCrearTurno() {
        servicioDeTurnos.crear(new SolicitudDeTurno(
                paciente, profA, sillon1, prestacion, hora(10, 0), OrigenTurno.ONLINE, false, Actor.paciente()));

        assertThatThrownBy(() -> reserva.reservar(new SolicitudDeReservaDeHueco(
                paciente, prestacion, profA, hora(10, 0), OrigenTurno.ONLINE, Actor.paciente())))
                .isInstanceOf(HuecoNoDisponibleException.class);

        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, modificado_en) "
                + "VALUES ('CONSULTORIO', NULL, NULL, ?, ?, 'FERIADO', ?, ?)",
                Timestamp.from(hora(11, 0)), Timestamp.from(hora(12, 0)), usuario, Timestamp.from(AHORA));

        assertThatThrownBy(() -> reserva.reservar(new SolicitudDeReservaDeHueco(
                paciente, prestacion, null, hora(11, 0), OrigenTurno.ONLINE, Actor.paciente())))
                .isInstanceOf(HuecoNoDisponibleException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM turno WHERE paciente_id = ?", Long.class, paciente))
                .isEqualTo(1L);
    }
}

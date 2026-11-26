package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.CandidatoDeReserva;
import ar.dentaflow.dominio.disponibilidad.Hueco;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
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
 * Integración de {@link MotorDeDisponibilidad} con la base y {@link ServicioDeTurnos} (tarea 4.4).
 */
@SpringBootTest
@ActiveProfiles("test")
class MotorDeDisponibilidadIntegracionTest {

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
    private MotorDeDisponibilidad motor;

    @Autowired
    private ServicioDeTurnos servicioDeTurnos;

    @Autowired
    private JdbcTemplate jdbc;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long profA;
    private long profB;
    private long profInactivo;
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
        profInactivo = escenario.profesional();
        jdbc.update("UPDATE profesional SET activo = false WHERE id = ?", profInactivo);

        sillon1 = escenario.sillon();
        sillon2 = escenario.sillon();
        prestacion = escenario.prestacion(30);
        escenario.habilitar(profA, prestacion);
        escenario.habilitar(profB, prestacion);
        escenario.habilitar(profInactivo, prestacion);
        usuario = escenario.usuario();

        // Lunes: profA 09:00-10:30, profB 09:30-10:30, profInactivo 09:00-11:00
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 1, '09:00:00', '10:30:00')", profA);
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 1, '09:30:00', '10:30:00')", profB);
        jdbc.update("INSERT INTO horario_atencion (profesional_id, dia_semana, hora_inicio, hora_fin) "
                + "VALUES (?, 1, '09:00:00', '11:00:00')", profInactivo);

        // Bloqueo del sillón 1 de 09:00 a 10:00
        jdbc.update("INSERT INTO bloqueo (ambito, profesional_id, sillon_id, inicio, fin, motivo, modificado_por, modificado_en) "
                + "VALUES ('SILLON', NULL, ?, ?, ?, 'OTRO', ?, ?)",
                sillon1, Timestamp.from(hora(9, 0)), Timestamp.from(hora(10, 0)), usuario, Timestamp.from(AHORA));

        // Turno común de profA 09:30-10:00 y sobreturno de profA 10:00-10:30 (no ocupa)
        escenario.turno(paciente, profA, sillon2, prestacion, hora(9, 30), hora(10, 0),
                "pendiente", "online", false, null, escenario.tokenNuevo());
        escenario.turno(paciente, profA, sillon2, prestacion, hora(10, 0), hora(10, 30),
                "pendiente", "recepcion", true, usuario, escenario.tokenNuevo());
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
    void calculaHuecosRealesYCadaHuecoOfrecidoSePuedeCrearConSuPrimerCandidatoYDejaDeOfrecerseConEl() {
        List<Hueco> huecos = motor.buscarHuecos(new ConsultaDeHuecos(prestacion, null, LUNES, LUNES, null));

        // 09:00 (profA, sillon2), 09:30 (profB, sillon2? no, sillon2 ocupado por profA 09:30-10:00 y sillon1 bloqueado -> sin sillon a las 09:30),
        // 09:45 (idem 09:30-10:00 ocupado sillon2 y sillon1 bloqueado -> sin sillon), 10:00 (profA/B, sillon1/2)
        assertThat(huecos).extracting(Hueco::inicio)
                .containsExactly(hora(9, 0), hora(10, 0));
        assertThat(huecos.get(0).candidatos()).containsExactly(new CandidatoDeReserva(profA, sillon2));
        assertThat(huecos.get(1).candidatos()).containsExactly(
                new CandidatoDeReserva(profA, sillon1),
                new CandidatoDeReserva(profA, sillon2),
                new CandidatoDeReserva(profB, sillon1),
                new CandidatoDeReserva(profB, sillon2));

        // Coherencia: tomar el primer candidato de cada hueco ofrecido lo crea y lo saca de candidatos
        for (Hueco hueco : huecos) {
            CandidatoDeReserva primero = hueco.candidatos().get(0);
            servicioDeTurnos.crear(new SolicitudDeTurno(paciente, primero.profesionalId(), primero.sillonId(),
                    prestacion, hueco.inicio(), OrigenTurno.ONLINE, false, Actor.paciente()));

            assertThat(motor.candidatosPara(prestacion, null, hueco.inicio(), null))
                    .doesNotContain(primero);
        }
    }
}

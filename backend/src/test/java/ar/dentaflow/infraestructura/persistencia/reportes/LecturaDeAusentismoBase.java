package ar.dentaflow.infraestructura.persistencia.reportes;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.reportes.LecturaDeAusentismo;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.reportes.RegistroAsistenciaParaReporte;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec panel-ausentismo — lectura de turnos evaluados en H2 y MySQL 8: límites de fecha argentina, solo estados de
 * asistencia, filtro por profesional, paciente anonimizado y proyección sin datos sensibles (Ley 25.326).
 */
@Transactional
abstract class LecturaDeAusentismoBase {

    /** Martes 4/3/2031 en el consultorio: 2031-03-04T03:00Z a 2031-03-05T03:00Z. */
    private static final Instant DESDE = Instant.parse("2031-03-04T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2031-03-05T03:00:00Z");

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private LecturaDeAusentismo lectura;

    @Autowired
    private EntityManager em;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long laura;
    private long martin;
    private long sillon;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void preparar() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        laura = escenario.profesional();
        martin = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        usuario = escenario.usuario();
    }

    private void turno(long profesionalId, Instant inicio, EstadoTurno estado) {
        Turno turno = Turno.nuevo(paciente, profesionalId, sillon, prestacion, inicio, 30, OrigenTurno.RECEPCION,
                false, usuario, Instant.parse("2031-01-01T12:00:00Z"), escenario.tokenNuevo());
        turno.cambiarEstado(estado);
        em.persist(turno);
        em.flush();
    }

    @Test
    void devuelve_solo_estados_de_asistencia_dentro_del_dia_del_consultorio() {
        turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.ASISTIO);
        turno(laura, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.AUSENTE);
        turno(laura, Instant.parse("2031-03-05T02:30:00Z"), EstadoTurno.AUSENTE_CON_AVISO); // 23:30 del 4/3 AR
        // Excluidos por estado:
        turno(laura, Instant.parse("2031-03-04T14:00:00Z"), EstadoTurno.PENDIENTE);
        turno(laura, Instant.parse("2031-03-04T15:00:00Z"), EstadoTurno.CONFIRMADO);
        turno(laura, Instant.parse("2031-03-04T16:00:00Z"), EstadoTurno.CANCELADO);
        // Excluidos por borde horario argentino:
        turno(laura, Instant.parse("2031-03-04T02:59:00Z"), EstadoTurno.AUSENTE); // 23:59 del 3/3 AR
        turno(laura, Instant.parse("2031-03-05T03:00:00Z"), EstadoTurno.AUSENTE); // 00:00 del 5/3 AR

        List<RegistroAsistenciaParaReporte> registros = lectura.buscarEvaluados(DESDE, HASTA, laura);

        assertThat(registros)
                .extracting(RegistroAsistenciaParaReporte::estado)
                .containsExactly(EstadoTurno.ASISTIO, EstadoTurno.AUSENTE, EstadoTurno.AUSENTE_CON_AVISO);
    }

    @Test
    void filtra_por_profesional_y_proyecta_sin_datos_sensibles() {
        turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.AUSENTE);
        turno(martin, Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.ASISTIO);

        List<RegistroAsistenciaParaReporte> deLaura = lectura.buscarEvaluados(DESDE, HASTA, laura);
        List<RegistroAsistenciaParaReporte> todos = lectura.buscarEvaluados(DESDE, HASTA, null).stream()
                .filter(r -> List.of(laura, martin).contains(r.profesionalId()))
                .toList();

        assertThat(deLaura).hasSize(1);
        assertThat(deLaura.get(0).profesionalId()).isEqualTo(laura);
        assertThat(deLaura.get(0).pacienteId()).isEqualTo(paciente);
        assertThat(deLaura.get(0).pacienteNombre()).isEqualTo("Ana");
        assertThat(deLaura.get(0).pacienteApellido()).isEqualTo("Ejemplo");
        assertThat(todos).hasSize(2);

        assertThat(RegistroAsistenciaParaReporte.class.getRecordComponents())
                .extracting(c -> c.getName().toLowerCase())
                .noneMatch(n -> n.contains("dni") || n.contains("telefono") || n.contains("email") || n.contains("token"));
    }

    @Test
    void paciente_anonimizado_aparece_con_valores_sustitutos() {
        em.find(Paciente.class, paciente).anonimizar(usuario, Instant.parse("2031-01-01T12:00:00Z"));
        em.flush();
        turno(laura, Instant.parse("2031-03-04T12:00:00Z"), EstadoTurno.AUSENTE);

        RegistroAsistenciaParaReporte registro = lectura.buscarEvaluados(DESDE, HASTA, laura).get(0);

        assertThat(registro.pacienteNombre()).isEqualTo("Paciente");
        assertThat(registro.pacienteApellido()).isEqualTo("Anonimizado");
    }
}

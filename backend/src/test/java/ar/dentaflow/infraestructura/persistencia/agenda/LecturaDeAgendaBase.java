package ar.dentaflow.infraestructura.persistencia.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.aplicacion.agenda.CriterioDeAgenda;
import ar.dentaflow.aplicacion.agenda.FilaDeAgenda;
import ar.dentaflow.aplicacion.agenda.LecturaDeAgenda;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spec agenda-interna — lectura en la base: cruce de intervalos semiabiertos, bordes de medianoche del consultorio
 * (UTC-3), filtros, orden y paciente anonimizado (C-15). Las subclases eligen el motor; todo se revierte al terminar.
 * Usa fechas de 2031 para no cruzarse con datos de una base local de desarrollo.
 */
@Transactional
abstract class LecturaDeAgendaBase {

    /** Martes 4/3/2031 en el consultorio: de 2031-03-04T03:00Z a 2031-03-05T03:00Z. */
    private static final Instant DESDE = Instant.parse("2031-03-04T03:00:00Z");
    private static final Instant HASTA = Instant.parse("2031-03-05T03:00:00Z");

    private static final List<EstadoTurno> SIN_CANCELADOS = Arrays.stream(EstadoTurno.values())
            .filter(estado -> estado != EstadoTurno.CANCELADO)
            .toList();

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private LecturaDeAgenda lectura;

    @Autowired
    private EntityManager em;

    private EscenarioDeAgendaJdbc escenario;
    private long paciente;
    private long laura;
    private long sillon;
    private long prestacion;
    private long usuario;

    @BeforeEach
    void crearRecursos() {
        escenario = new EscenarioDeAgendaJdbc(jdbc);
        paciente = escenario.paciente();
        laura = escenario.profesional();
        sillon = escenario.sillon();
        prestacion = escenario.prestacion(30);
        usuario = escenario.usuario();
    }

    /**
     * Crea el turno con el modelo de C-09 y lo persiste con Hibernate, que guarda los instantes en UTC igual que en
     * producción. (Un {@code INSERT} JDBC con {@code Timestamp.from} se corre 3 horas en MySQL Connector/J y uno con
     * {@code LocalDateTime} se corre en H2: ninguno representa lo que escribe la app.)
     */
    private long turno(long profesional, long sillonId, Instant inicio, Instant fin, String estado) {
        int minutos = (int) Duration.between(inicio, fin).toMinutes();
        Turno turno = Turno.nuevo(paciente, profesional, sillonId, prestacion, inicio, minutos, OrigenTurno.RECEPCION,
                false, usuario, Instant.parse("2031-01-01T12:00:00Z"), escenario.tokenNuevo());
        turno.cambiarEstado(EstadoTurno.desdeCodigo(estado));
        em.persist(turno);
        em.flush();
        return turno.getId();
    }

    private long profesional(String nombre, String apellido) {
        String email = "agenda-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        jdbc.update("INSERT INTO profesional (nombre, apellido, email) VALUES (?, ?, ?)", nombre, apellido, email);
        return jdbc.queryForObject("SELECT id FROM profesional WHERE email = ?", Long.class, email);
    }

    private long sillon(String nombre) {
        String unico = nombre + " " + UUID.randomUUID().toString().substring(0, 6);
        jdbc.update("INSERT INTO sillon (nombre) VALUES (?)", unico);
        return jdbc.queryForObject("SELECT id FROM sillon WHERE nombre = ?", Long.class, unico);
    }

    private List<FilaDeAgenda> delProfesional(long profesional) {
        return lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, profesional, null, SIN_CANCELADOS));
    }

    private static List<Long> ids(List<FilaDeAgenda> filas) {
        return filas.stream().map(FilaDeAgenda::turnoId).toList();
    }

    @Test
    void incluye_los_turnos_que_se_cruzan_con_el_dia_del_consultorio() {
        long mediodia = turno(laura, sillon, Instant.parse("2031-03-04T12:00:00Z"),
                Instant.parse("2031-03-04T12:30:00Z"), "confirmado");
        long once_y_media_de_la_noche = turno(laura, sillon, Instant.parse("2031-03-05T02:30:00Z"),
                Instant.parse("2031-03-05T03:00:00Z"), "pendiente");
        turno(laura, sillon, Instant.parse("2031-03-05T03:00:00Z"), Instant.parse("2031-03-05T03:30:00Z"),
                "pendiente"); // 00:00 del día siguiente en el consultorio
        turno(laura, sillon, Instant.parse("2031-03-04T02:30:00Z"), Instant.parse("2031-03-04T03:00:00Z"),
                "pendiente"); // 23:30 del día anterior: termina justo cuando empieza el rango

        assertThat(ids(delProfesional(laura))).containsExactly(mediodia, once_y_media_de_la_noche);
    }

    @Test
    void devuelve_los_datos_de_la_agenda_sin_datos_sensibles_del_paciente() {
        long id = turno(laura, sillon, Instant.parse("2031-03-04T12:00:00Z"), Instant.parse("2031-03-04T12:30:00Z"),
                "confirmado");

        FilaDeAgenda fila = delProfesional(laura).get(0);
        assertThat(fila.turnoId()).isEqualTo(id);
        assertThat(fila.inicio()).isEqualTo(Instant.parse("2031-03-04T12:00:00Z"));
        assertThat(fila.fin()).isEqualTo(Instant.parse("2031-03-04T12:30:00Z"));
        assertThat(fila.estado()).isEqualTo(EstadoTurno.CONFIRMADO);
        assertThat(fila.origen()).isEqualTo(OrigenTurno.RECEPCION);
        assertThat(fila.esSobreturno()).isFalse();
        assertThat(fila.pacienteNombre()).isEqualTo("Ana");
        assertThat(fila.pacienteApellido()).isEqualTo("Ejemplo");
        assertThat(fila.profesionalId()).isEqualTo(laura);
        assertThat(fila.profesionalNombre()).isEqualTo("Laura");
        assertThat(fila.profesionalApellido()).isEqualTo("Ejemplo");
        assertThat(fila.sillonId()).isEqualTo(sillon);
        assertThat(fila.sillonNombre()).startsWith("Sillon-");
        assertThat(fila.prestacionId()).isEqualTo(prestacion);
        assertThat(fila.prestacionNombre()).startsWith("Prestacion-");
        assertThat(FilaDeAgenda.class.getRecordComponents())
                .extracting(componente -> componente.getName().toLowerCase())
                .noneMatch(nombre -> nombre.contains("dni") || nombre.contains("telefono")
                        || nombre.contains("email") || nombre.contains("token"));
    }

    @Test
    void un_paciente_anonimizado_aparece_con_sus_valores_sustitutos() {
        Paciente aAnonimizar = em.find(Paciente.class, paciente);
        aAnonimizar.anonimizar(usuario, Instant.parse("2031-01-01T12:00:00Z"));
        em.flush();
        turno(laura, sillon, Instant.parse("2031-03-04T12:00:00Z"), Instant.parse("2031-03-04T12:30:00Z"),
                "asistio");

        FilaDeAgenda fila = delProfesional(laura).get(0);
        assertThat(fila.pacienteNombre()).isEqualTo("Paciente");
        assertThat(fila.pacienteApellido()).isEqualTo("Anonimizado");
    }

    @Test
    void ordena_por_inicio_y_a_igual_hora_por_profesional_y_sillon() {
        long diaz = profesional("Marta", "Díaz");
        long sillonA = sillon("A");
        long sillonB = sillon("B");
        Instant diez = Instant.parse("2031-03-04T13:00:00Z");
        Instant diezYMedia = Instant.parse("2031-03-04T13:30:00Z");
        long lauraEnB = turno(laura, sillonB, diez, diezYMedia, "confirmado");
        long lauraEnA = turno(laura, sillonA, diez, Instant.parse("2031-03-04T13:20:00Z"), "confirmado");
        long diazALasDiez = turno(diaz, sillonB, diez, diezYMedia, "pendiente");
        long diazTemprano = turno(diaz, sillonA, Instant.parse("2031-03-04T12:30:00Z"), diez, "pendiente");

        List<FilaDeAgenda> filas = lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, null, null, SIN_CANCELADOS))
                .stream()
                .filter(fila -> List.of(laura, diaz).contains(fila.profesionalId()))
                .toList();

        assertThat(ids(filas)).containsExactly(diazTemprano, diazALasDiez, lauraEnA, lauraEnB);
    }

    @Test
    void filtra_por_sillon_y_por_estado() {
        long otroSillon = sillon("Otro");
        long enOtroSillon = turno(laura, otroSillon, Instant.parse("2031-03-04T12:00:00Z"),
                Instant.parse("2031-03-04T12:30:00Z"), "pendiente");
        long cancelado = turno(laura, sillon, Instant.parse("2031-03-04T13:00:00Z"),
                Instant.parse("2031-03-04T13:30:00Z"), "cancelado");
        long confirmado = turno(laura, sillon, Instant.parse("2031-03-04T14:00:00Z"),
                Instant.parse("2031-03-04T14:30:00Z"), "confirmado");

        assertThat(ids(lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, laura, otroSillon, SIN_CANCELADOS))))
                .containsExactly(enOtroSillon);
        assertThat(ids(delProfesional(laura))).containsExactly(enOtroSillon, confirmado);
        assertThat(ids(lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, laura, null, List.of(EstadoTurno.CANCELADO)))))
                .containsExactly(cancelado);
        assertThat(ids(lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, laura, sillon, List.of(EstadoTurno.PENDIENTE)))))
                .isEmpty();
    }

    @Test
    void un_profesional_o_sillon_inexistente_devuelve_lista_vacia() {
        turno(laura, sillon, Instant.parse("2031-03-04T12:00:00Z"), Instant.parse("2031-03-04T12:30:00Z"),
                "confirmado");

        assertThat(lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, -1L, null, SIN_CANCELADOS))).isEmpty();
        assertThat(lectura.buscar(new CriterioDeAgenda(DESDE, HASTA, laura, -1L, SIN_CANCELADOS))).isEmpty();
    }
}

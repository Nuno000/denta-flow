package ar.dentaflow.aplicacion.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.aplicacion.turnos.SolicitudDeTurno;
import ar.dentaflow.aplicacion.turnos.TurnoCreado;
import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencion;
import ar.dentaflow.dominio.disponibilidad.HorarioAtencionRepositorio;
import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OferenteYAceptadorListaEsperaIntegracionTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ServicioDeTurnos turnos;
    @Autowired
    private ServicioDeListaDeEspera servicioLista;
    @Autowired
    private AceptadorDeOfertaListaEspera aceptador;
    @Autowired
    private ListaEsperaRepositorio listaEspera;
    @Autowired
    private PacienteRepositorio pacientes;
    @Autowired
    private PrestacionRepositorio prestaciones;
    @Autowired
    private ProfesionalRepositorio profesionales;
    @Autowired
    private SillonRepositorio sillones;
    @Autowired
    private HorarioAtencionRepositorio horarios;
    @Autowired
    private ServicioDeUsuarios usuarios;
    @Autowired
    private JdbcTemplate jdbc;

    private long usuarioId;
    private long prestacionId;
    private long profesionalId;
    private long otroProfesionalId;
    private long sillonId;
    private long paciente1Id;
    private long paciente2Id;
    private long paciente3Id;
    private Instant inicioTurno;
    private LocalDate fechaTurno;

    @BeforeEach
    void preparar() {
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        usuarioId = usuarios.crear("recep-" + sufijo + "@example.com", "contrasena-valida", Rol.RECEPCION, null)
                .usuario().getId();

        Prestacion prestacion = prestaciones.guardar(Prestacion.nueva("Limpieza " + sufijo, 30));
        prestacionId = prestacion.getId();

        Profesional prof = Profesional.nuevo("Maria", "Lopez", "MP1-" + sufijo, "maria-" + sufijo + "@example.com");
        prof.habilitarPrestacion(prestacion);
        profesionalId = profesionales.guardar(prof).getId();

        Profesional otroProf = Profesional.nuevo("Juan", "Perez", "MP2-" + sufijo, "juan-" + sufijo + "@example.com");
        otroProf.habilitarPrestacion(prestacion);
        otroProfesionalId = profesionales.guardar(otroProf).getId();

        sillonId = sillones.guardar(Sillon.nuevo("Sillon " + sufijo)).getId();

        fechaTurno = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(3);
        DayOfWeek dia = fechaTurno.getDayOfWeek();
        horarios.guardar(HorarioAtencion.nuevo(profesionalId, dia, LocalTime.of(8, 0), LocalTime.of(18, 0)));

        inicioTurno = ZonedDateTime.of(fechaTurno, LocalTime.of(10, 0), ZonaHorariaConsultorio.ZONA).toInstant();

        String baseDni = String.valueOf(31000000 + Math.abs(sufijo.hashCode() % 8000000));
        paciente1Id = pacientes.guardar(Paciente.nuevo(
                baseDni, "Ana", "Gomez", "1122334455", "ana-" + sufijo + "@example.com", Instant.now())).getId();
        String baseDni2 = String.valueOf(32000000 + Math.abs(sufijo.hashCode() % 8000000));
        paciente2Id = pacientes.guardar(Paciente.nuevo(
                baseDni2, "Luis", "Paz", "1122334466", "luis-" + sufijo + "@example.com", Instant.now())).getId();
        String baseDni3 = String.valueOf(33000000 + Math.abs(sufijo.hashCode() % 8000000));
        paciente3Id = pacientes.guardar(Paciente.nuevo(
                baseDni3, "Sofia", "Diaz", "1122334477", "sofia-" + sufijo + "@example.com", Instant.now())).getId();
    }

    @AfterEach
    void limpiar() {
        jdbc.update("DELETE FROM lista_espera WHERE paciente_id IN (?, ?, ?)", paciente1Id, paciente2Id, paciente3Id);
        jdbc.update("DELETE FROM turno_historial WHERE turno_id IN (SELECT id FROM turno WHERE paciente_id IN (?, ?, ?))",
                paciente1Id, paciente2Id, paciente3Id);
        jdbc.update("DELETE FROM turno WHERE paciente_id IN (?, ?, ?)", paciente1Id, paciente2Id, paciente3Id);
        jdbc.update("DELETE FROM horario_atencion WHERE profesional_id IN (?, ?)", profesionalId, otroProfesionalId);
        jdbc.update("DELETE FROM profesional_prestacion WHERE profesional_id IN (?, ?)", profesionalId, otroProfesionalId);
        jdbc.update("DELETE FROM paciente WHERE id IN (?, ?, ?)", paciente1Id, paciente2Id, paciente3Id);
        jdbc.update("DELETE FROM profesional WHERE id IN (?, ?)", profesionalId, otroProfesionalId);
        jdbc.update("DELETE FROM sillon WHERE id = ?", sillonId);
        jdbc.update("DELETE FROM prestacion WHERE id = ?", prestacionId);
        jdbc.update("DELETE FROM usuario WHERE id = ?", usuarioId);
    }

    @Test
    void alCancelarTurnoOfreceACompatiblesYAceptacionPublicaReservaYVenceOtras() throws Exception {
        TurnoCreado turno = turnos.crear(new SolicitudDeTurno(
                paciente1Id, profesionalId, sillonId, prestacionId, inicioTurno,
                OrigenTurno.ONLINE, false, Actor.paciente()));

        ListaEsperaDetalle compatible = servicioLista.registrar(new SolicitudListaEspera(
                paciente2Id, prestacionId, null, fechaTurno, fechaTurno.plusDays(2),
                LocalTime.of(9, 0), LocalTime.of(12, 0)), usuarioId);

        ListaEsperaDetalle otroProf = servicioLista.registrar(new SolicitudListaEspera(
                paciente3Id, prestacionId, otroProfesionalId, fechaTurno, fechaTurno.plusDays(2),
                null, null), usuarioId);

        // Otro compatible que también compite
        ListaEsperaDetalle compatible2 = servicioLista.registrar(new SolicitudListaEspera(
                paciente1Id, prestacionId, profesionalId, fechaTurno, fechaTurno.plusDays(2),
                null, null), usuarioId);

        turnos.cancelar(turno.turno().getId(), Actor.paciente());

        ListaEspera ofertada1 = listaEspera.buscarPorId(compatible.id()).orElseThrow();
        ListaEspera ofertada2 = listaEspera.buscarPorId(compatible2.id()).orElseThrow();
        ListaEspera noCompatible = listaEspera.buscarPorId(otroProf.id()).orElseThrow();

        assertThat(ofertada1.getEstado()).isEqualTo(EstadoListaEspera.OFRECIDA);
        assertThat(ofertada2.getEstado()).isEqualTo(EstadoListaEspera.OFRECIDA);
        assertThat(noCompatible.getEstado()).isEqualTo(EstadoListaEspera.ACTIVA);

        // Consulta pública y aceptación vía MockMvc
        mockMvc.perform(get("/api/publico/lista-espera/" + ofertada1.getTokenAceptacion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ofrecida"));

        mockMvc.perform(post("/api/publico/lista-espera/" + ofertada1.getTokenAceptacion() + "/aceptar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("atendida"))
                .andExpect(jsonPath("$.turnoCreadoId").isNumber());

        // La segunda oferta quedó vencida automáticamente y no puede aceptarse
        assertThat(listaEspera.buscarPorId(ofertada2.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoListaEspera.VENCIDA);

        mockMvc.perform(post("/api/publico/lista-espera/" + ofertada2.getTokenAceptacion() + "/aceptar"))
                .andExpect(status().isConflict());
    }

    @Test
    void rechazaOfertaVencida() {
        TurnoCreado turno = turnos.crear(new SolicitudDeTurno(
                paciente1Id, profesionalId, sillonId, prestacionId, inicioTurno,
                OrigenTurno.ONLINE, false, Actor.paciente()));
        turnos.cancelar(turno.turno().getId(), Actor.paciente());

        String token = "tok-exp-" + UUID.randomUUID();
        ListaEspera entrada = ListaEspera.nueva(
                paciente2Id, prestacionId, profesionalId, fechaTurno, fechaTurno, null, null, usuarioId, Instant.now());
        entrada.ofrecer(turno.turno().getId(), profesionalId, inicioTurno, token,
                Instant.now().minusSeconds(7200), Instant.now().minusSeconds(60));
        listaEspera.guardar(entrada);

        assertThatThrownBy(() -> aceptador.aceptar(token))
                .isInstanceOf(ConflictoException.class);
        assertThat(listaEspera.buscarPorId(entrada.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoListaEspera.VENCIDA);
    }
}

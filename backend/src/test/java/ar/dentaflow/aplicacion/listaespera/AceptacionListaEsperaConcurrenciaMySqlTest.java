package ar.dentaflow.aplicacion.listaespera;

import static org.assertj.core.api.Assertions.assertThat;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spec lista-de-espera — "Dos pacientes aceptan el mismo hueco simultáneamente" contra MySQL 8 real
 * (perfil {@code it-mysql}).
 */
@Tag("mysql")
@SpringBootTest
@ActiveProfiles("it-mysql")
class AceptacionListaEsperaConcurrenciaMySqlTest {

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

    @Test
    void dosAceptacionesSimultaneasSoloCreanUnTurnoYSinSolapamiento() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 6);
        long usuarioId = usuarios.crear("mysql-le-" + sufijo + "@example.com", "contrasena-valida",
                Rol.RECEPCION, null).usuario().getId();
        Prestacion prestacion = prestaciones.guardar(Prestacion.nueva("Limpieza MySQL " + sufijo, 30));
        Profesional prof = Profesional.nuevo("Maria", "Lopez", "MMY-" + sufijo,
                "maria-my-" + sufijo + "@example.com");
        prof.habilitarPrestacion(prestacion);
        long profesionalId = profesionales.guardar(prof).getId();
        long sillonId = sillones.guardar(Sillon.nuevo("Sillon MySQL " + sufijo)).getId();

        LocalDate fecha = LocalDate.now(ZonaHorariaConsultorio.ZONA).plusDays(4);
        horarios.guardar(HorarioAtencion.nuevo(profesionalId, fecha.getDayOfWeek(),
                LocalTime.of(8, 0), LocalTime.of(18, 0)));
        Instant inicio = ZonedDateTime.of(fecha, LocalTime.of(11, 0), ZonaHorariaConsultorio.ZONA).toInstant();

        long p1 = pacientes.guardar(Paciente.nuevo(
                String.valueOf(41000000 + Math.abs(sufijo.hashCode() % 8000000)),
                "Ana", "Gomez", "1122334455", "ana-my-" + sufijo + "@example.com", Instant.now())).getId();
        long p2 = pacientes.guardar(Paciente.nuevo(
                String.valueOf(42000000 + Math.abs(sufijo.hashCode() % 8000000)),
                "Luis", "Paz", "1122334466", "luis-my-" + sufijo + "@example.com", Instant.now())).getId();
        long p3 = pacientes.guardar(Paciente.nuevo(
                String.valueOf(43000000 + Math.abs(sufijo.hashCode() % 8000000)),
                "Sofia", "Diaz", "1122334477", "sofia-my-" + sufijo + "@example.com", Instant.now())).getId();

        TurnoCreado turno = turnos.crear(new SolicitudDeTurno(
                p1, profesionalId, sillonId, prestacion.getId(), inicio,
                OrigenTurno.ONLINE, false, Actor.paciente()));

        ListaEsperaDetalle e2 = servicioLista.registrar(new SolicitudListaEspera(
                p2, prestacion.getId(), profesionalId, fecha, fecha, null, null), usuarioId);
        ListaEsperaDetalle e3 = servicioLista.registrar(new SolicitudListaEspera(
                p3, prestacion.getId(), profesionalId, fecha, fecha, null, null), usuarioId);

        turnos.cancelar(turno.turno().getId(), Actor.paciente());

        String tok2 = listaEspera.buscarPorId(e2.id()).orElseThrow().getTokenAceptacion();
        String tok3 = listaEspera.buscarPorId(e3.id()).orElseThrow().getTokenAceptacion();

        CountDownLatch listo = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> f1 = pool.submit(() -> {
                listo.await(5, TimeUnit.SECONDS);
                try {
                    aceptador.aceptar(tok2);
                    return true;
                } catch (ConflictoException ex) {
                    return false;
                }
            });
            Future<Boolean> f2 = pool.submit(() -> {
                listo.await(5, TimeUnit.SECONDS);
                try {
                    aceptador.aceptar(tok3);
                    return true;
                } catch (ConflictoException ex) {
                    return false;
                }
            });
            listo.countDown();

            List<Boolean> resultados = List.of(f1.get(15, TimeUnit.SECONDS), f2.get(15, TimeUnit.SECONDS));
            assertThat(resultados).containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }

        EstadoListaEspera est2 = listaEspera.buscarPorId(e2.id()).orElseThrow().getEstado();
        EstadoListaEspera est3 = listaEspera.buscarPorId(e3.id()).orElseThrow().getEstado();
        assertThat(List.of(est2, est3)).containsExactlyInAnyOrder(EstadoListaEspera.ATENDIDA, EstadoListaEspera.VENCIDA);
    }
}

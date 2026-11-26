package ar.dentaflow.infraestructura.persistencia.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.HistorialDeTurnoRepositorio;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import ar.dentaflow.escenarios.EscenarioDeAgendaJdbc;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Spec auditoria-turnos — persistencia del historial (design D3 y D4). */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HistorialDeTurnoRepositorioTest {

    private static final Instant DOCE = Instant.parse("2026-12-01T12:00:00Z");

    @Autowired
    private HistorialDeTurnoRepositorio historial;

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private EntityManager em;

    @Autowired
    private JdbcTemplate jdbc;

    /** Turnos reales (FK desde C-09), propios de cada prueba: la base del perfil test se comparte entre clases. */
    private long turno;
    private long otroTurno;

    @BeforeEach
    void crearTurnos() {
        EscenarioDeAgendaJdbc escenario = new EscenarioDeAgendaJdbc(jdbc);
        turno = escenario.turnoValido();
        otroTurno = escenario.turnoValido();
    }

    private void sincronizar() {
        em.flush();
        em.clear();
    }

    private CambioDeTurno cambio(long turnoId, Instant cuando, AccionDeTurno accion) {
        Map<String, String> anterior = accion == AccionDeTurno.CREACION ? null : Map.of("estado", "pendiente");
        return CambioDeTurno.registrar(turnoId, Actor.sistema(), cuando, accion, anterior,
                Map.of("estado", "confirmado"));
    }

    @Test
    void agregaYDevuelveElRegistroConSuId() {
        Long usuario = usuarios.guardar(Usuario.nuevo("auditoria-" + UUID.randomUUID().toString().substring(0, 8)
                + "@example.com", "$2a$10$hashDePrueba", Rol.RECEPCION, null)).getId();
        Instant conMicros = Instant.parse("2026-12-01T13:15:00.123456Z");

        CambioDeTurno guardado = historial.agregar(CambioDeTurno.registrar(turno, Actor.usuario(usuario), conMicros,
                AccionDeTurno.CAMBIO_ESTADO, Map.of("estado", "pendiente"), Map.of("estado", "confirmado")));
        sincronizar();

        assertThat(guardado.id()).isNotNull();
        CambioDeTurno leido = historial.historialDe(turno).getFirst();
        assertThat(leido.actor()).isEqualTo(Actor.usuario(usuario));
        assertThat(leido.ocurridoEn()).isEqualTo(conMicros);
        assertThat(leido.accion()).isEqualTo(AccionDeTurno.CAMBIO_ESTADO);
        assertThat(leido.valorAnterior()).containsExactly(Map.entry("estado", "pendiente"));
        assertThat(leido.valorNuevo()).containsExactly(Map.entry("estado", "confirmado"));
    }

    @Test
    void laCreacionGuardaElValorAnteriorVacio() {
        historial.agregar(cambio(turno, DOCE, AccionDeTurno.CREACION));
        sincronizar();

        assertThat(historial.historialDe(turno).getFirst().valorAnterior()).isEmpty();
        assertThat(em.createNativeQuery("SELECT valor_anterior FROM turno_historial WHERE turno_id = ?1")
                .setParameter(1, turno).getSingleResult()).isNull();
    }

    @Test
    void devuelveElHistorialDeUnTurnoEnOrdenCronologicoYSinOtrosTurnos() {
        historial.agregar(cambio(turno, DOCE.plusSeconds(30 * 60), AccionDeTurno.CANCELACION));
        historial.agregar(cambio(turno, DOCE, AccionDeTurno.CREACION));
        historial.agregar(cambio(otroTurno, DOCE.plusSeconds(60), AccionDeTurno.CONFIRMACION));
        historial.agregar(cambio(turno, DOCE.plusSeconds(5 * 60), AccionDeTurno.CONFIRMACION));
        sincronizar();

        assertThat(historial.historialDe(turno)).extracting(CambioDeTurno::accion).containsExactly(
                AccionDeTurno.CREACION, AccionDeTurno.CONFIRMACION, AccionDeTurno.CANCELACION);
    }

    @Test
    void aIgualInstanteRespetaElOrdenDeAlta() {
        historial.agregar(cambio(turno, DOCE, AccionDeTurno.CONFIRMACION));
        historial.agregar(cambio(turno, DOCE, AccionDeTurno.CANCELACION));
        historial.agregar(cambio(turno, DOCE, AccionDeTurno.REPROGRAMACION));
        sincronizar();

        assertThat(historial.historialDe(turno)).extracting(CambioDeTurno::accion).containsExactly(
                AccionDeTurno.CONFIRMACION, AccionDeTurno.CANCELACION, AccionDeTurno.REPROGRAMACION);
    }

    @Test
    void unTurnoSinRegistrosDevuelveListaVacia() {
        assertThat(historial.historialDe(turno)).isEmpty();
    }

    @Test
    void noVuelveAAgregarUnRegistroQueYaTieneId() {
        CambioDeTurno guardado = historial.agregar(cambio(turno, DOCE, AccionDeTurno.CREACION));

        // Spring traduce la IllegalArgumentException del repositorio a InvalidDataAccessApiUsageException.
        assertThatThrownBy(() -> historial.agregar(guardado))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("solo agrega registros nuevos");
    }

    @Test
    void unRegistroGuardadoNoSeModificaAunqueSeCambieLaEntidad() throws Exception {
        CambioDeTurno guardado = historial.agregar(cambio(turno, DOCE, AccionDeTurno.CONFIRMACION));
        sincronizar();
        CambioDeTurno gestionado = em.find(CambioDeTurno.class, guardado.id());

        escribir(gestionado, "accion", AccionDeTurno.CANCELACION);
        escribir(gestionado, "valorNuevo", Map.of("estado", "cancelado"));
        escribir(gestionado, "ocurridoEn", DOCE.plusSeconds(3600));
        sincronizar();

        List<CambioDeTurno> leido = historial.historialDe(turno);
        assertThat(leido).singleElement().satisfies(registro -> {
            assertThat(registro.accion()).isEqualTo(AccionDeTurno.CONFIRMACION);
            assertThat(registro.valorNuevo()).containsExactly(Map.entry("estado", "confirmado"));
            assertThat(registro.ocurridoEn()).isEqualTo(DOCE);
        });
    }

    private static void escribir(Object objeto, String campo, Object valor) throws ReflectiveOperationException {
        Field field = CambioDeTurno.class.getDeclaredField(campo);
        field.setAccessible(true);
        field.set(objeto, valor);
    }
}

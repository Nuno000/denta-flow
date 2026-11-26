package ar.dentaflow.aplicacion.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.auditoria.CambioDeTurno;
import ar.dentaflow.dominio.auditoria.TipoDeActor;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec auditoria-turnos (US-025) — caso de uso de consulta de historial por turno (design D1). */
class ConsultaDeHistorialDeTurnoTest {

    private static final long TURNO_ID = 42L;
    private static final long USUARIO_ID = 7L;

    private ServicioDeTurnos turnos;
    private ServicioDeAuditoria auditoria;
    private UsuarioRepositorio usuarios;
    private ConsultaDeHistorialDeTurno consulta;
    private Turno turno;

    @BeforeEach
    void preparar() {
        turnos = mock(ServicioDeTurnos.class);
        auditoria = mock(ServicioDeAuditoria.class);
        usuarios = mock(UsuarioRepositorio.class);
        consulta = new ConsultaDeHistorialDeTurno(turnos, auditoria, usuarios);
        turno = Turno.nuevo(1L, 10L, 2L, 3L, Instant.parse("2026-12-01T12:00:00Z"), 30, OrigenTurno.ONLINE, false, null,
                Instant.parse("2026-11-25T12:00:00Z"), "token-prueba");
        when(turnos.buscarPorId(TURNO_ID)).thenReturn(Optional.of(turno));
    }

    @Test
    void si_el_turno_no_existe_lanza_404_sin_consultar_historial() {
        when(turnos.buscarPorId(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consulta.consultar(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);
        verify(auditoria, never()).historialDe(999L);
    }

    @Test
    void devuelve_entradas_en_orden_resolviendo_email_sin_repetir_consultas_y_null_en_paciente_o_sistema() {
        CambioDeTurno creacion = CambioDeTurno.registrar(TURNO_ID, Actor.paciente(),
                Instant.parse("2026-11-25T12:00:00Z"), AccionDeTurno.CREACION, Map.of(),
                Map.of("estado", "pendiente"));
        CambioDeTurno confirmacion = CambioDeTurno.registrar(TURNO_ID, Actor.usuario(USUARIO_ID),
                Instant.parse("2026-11-25T13:00:00Z"), AccionDeTurno.CONFIRMACION,
                Map.of("estado", "pendiente"), Map.of("estado", "confirmado"));
        CambioDeTurno asistencia = CambioDeTurno.registrar(TURNO_ID, Actor.usuario(USUARIO_ID),
                Instant.parse("2026-12-01T12:05:00Z"), AccionDeTurno.CAMBIO_ESTADO,
                Map.of("estado", "confirmado"), Map.of("estado", "asistio"));
        CambioDeTurno sistema = CambioDeTurno.registrar(TURNO_ID, Actor.sistema(),
                Instant.parse("2026-12-01T13:00:00Z"), AccionDeTurno.CANCELACION,
                Map.of("estado", "asistio"), Map.of("estado", "cancelado"));

        when(auditoria.historialDe(TURNO_ID)).thenReturn(List.of(creacion, confirmacion, asistencia, sistema));
        Usuario recepcionista = Usuario.nuevo("recepcion@example.com", "hash-valido", Rol.RECEPCION, null);
        when(usuarios.buscarPorId(USUARIO_ID)).thenReturn(Optional.of(recepcionista));

        List<EntradaDeHistorial> resultado = consulta.consultar(TURNO_ID);

        assertThat(resultado).hasSize(4);
        assertThat(resultado.get(0).actorTipo()).isEqualTo(TipoDeActor.PACIENTE);
        assertThat(resultado.get(0).actorEmail()).isNull();
        assertThat(resultado.get(0).accion()).isEqualTo(AccionDeTurno.CREACION);

        assertThat(resultado.get(1).actorTipo()).isEqualTo(TipoDeActor.USUARIO);
        assertThat(resultado.get(1).actorEmail()).isEqualTo("recepcion@example.com");
        assertThat(resultado.get(1).valorAnterior()).containsEntry("estado", "pendiente");
        assertThat(resultado.get(1).valorNuevo()).containsEntry("estado", "confirmado");

        assertThat(resultado.get(2).actorEmail()).isEqualTo("recepcion@example.com");
        assertThat(resultado.get(3).actorTipo()).isEqualTo(TipoDeActor.SISTEMA);
        assertThat(resultado.get(3).actorEmail()).isNull();

        verify(usuarios, times(1)).buscarPorId(USUARIO_ID);
    }

    @Test
    void turno_sin_cambios_devuelve_lista_vacia() {
        when(auditoria.historialDe(TURNO_ID)).thenReturn(List.of());

        assertThat(consulta.consultar(TURNO_ID)).isEmpty();
    }
}

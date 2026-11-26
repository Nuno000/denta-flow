package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

/** Spec asistencia-turnos — reglas del caso de uso (design D2). */
class RegistroDeAsistenciaTest {

    private static final long TURNO_ID = 41L;
    private static final long PROFESIONAL = 10L;
    private static final long USUARIO = 7L;
    /** 09:00 del consultorio (UTC-3). */
    private static final Instant INICIO = Instant.parse("2026-12-01T12:00:00Z");

    private ServicioDeTurnos turnos;
    private Turno turno;

    @BeforeEach
    void preparar() {
        turnos = mock(ServicioDeTurnos.class);
        turno = Turno.nuevo(1L, PROFESIONAL, 2L, 3L, INICIO, 30, OrigenTurno.ONLINE, false, null,
                Instant.parse("2026-11-20T12:00:00Z"), "token-de-prueba");
        turno.cambiarEstado(EstadoTurno.CONFIRMADO);
        when(turnos.buscarPorId(TURNO_ID)).thenReturn(Optional.of(turno));
        when(turnos.cambiarEstado(anyLong(), any(), any())).thenReturn(turno);
    }

    private RegistroDeAsistencia a(String instante) {
        return new RegistroDeAsistencia(turnos, Clock.fixed(Instant.parse(instante), ZoneOffset.UTC));
    }

    private RegistroDeAsistencia despuesDelInicio() {
        return a("2026-12-01T12:10:00Z");
    }

    @Nested
    class Estado {

        @ParameterizedTest
        @ValueSource(strings = {"confirmado", "cancelado", "pendiente", "borrado"})
        void un_estado_que_no_es_de_asistencia_se_rechaza_sin_repetir_el_valor(String estado) {
            assertThatThrownBy(() -> despuesDelInicio().registrar(TURNO_ID, estado, Rol.RECEPCION, USUARIO, null))
                    .isInstanceOf(ValidacionDeDominioException.class)
                    .hasMessageNotContaining(estado);
            verify(turnos, never()).cambiarEstado(anyLong(), any(), any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        void sin_estado_se_rechaza(String estado) {
            assertThatThrownBy(() -> despuesDelInicio().registrar(TURNO_ID, estado, Rol.RECEPCION, USUARIO, null))
                    .isInstanceOf(ValidacionDeDominioException.class);
        }

        @Test
        void un_turno_inexistente_da_no_encontrado() {
            when(turnos.buscarPorId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> despuesDelInicio().registrar(99L, "asistio", Rol.RECEPCION, USUARIO, null))
                    .isInstanceOf(RecursoNoEncontradoException.class);
        }
    }

    @Nested
    class Permisos {

        @Test
        void recepcion_y_administrador_pueden_en_cualquier_turno() {
            despuesDelInicio().registrar(TURNO_ID, "asistio", Rol.RECEPCION, USUARIO, null);
            despuesDelInicio().registrar(TURNO_ID, "ausente", Rol.ADMINISTRADOR, USUARIO, null);

            verify(turnos).cambiarEstado(TURNO_ID, EstadoTurno.ASISTIO, Actor.usuario(USUARIO));
            verify(turnos).cambiarEstado(TURNO_ID, EstadoTurno.AUSENTE, Actor.usuario(USUARIO));
        }

        @Test
        void el_profesional_puede_en_su_turno() {
            despuesDelInicio().registrar(TURNO_ID, "ausente", Rol.PROFESIONAL, USUARIO, PROFESIONAL);

            verify(turnos).cambiarEstado(TURNO_ID, EstadoTurno.AUSENTE, Actor.usuario(USUARIO));
        }

        @Test
        void el_profesional_no_puede_en_un_turno_ajeno() {
            assertThatThrownBy(() -> despuesDelInicio().registrar(TURNO_ID, "asistio", Rol.PROFESIONAL, USUARIO,
                    PROFESIONAL + 1))
                    .isInstanceOf(AccessDeniedException.class);
            verify(turnos, never()).cambiarEstado(anyLong(), any(), any());
        }

        @Test
        void el_profesional_sin_vinculo_no_puede() {
            assertThatThrownBy(() -> despuesDelInicio().registrar(TURNO_ID, "asistio", Rol.PROFESIONAL, USUARIO,
                    null))
                    .isInstanceOf(AccessDeniedException.class);
            verify(turnos, never()).cambiarEstado(anyLong(), any(), any());
        }
    }

    @Nested
    class Momento {

        @Test
        void asistio_antes_del_inicio_se_rechaza() {
            assertThatThrownBy(() -> a("2026-12-01T11:50:00Z").registrar(TURNO_ID, "asistio", Rol.RECEPCION,
                    USUARIO, null))
                    .isInstanceOf(OperacionNoPermitidaException.class)
                    .hasMessage("La asistencia se registra desde el inicio del turno");
            verify(turnos, never()).cambiarEstado(anyLong(), any(), any());
        }

        @Test
        void ausente_antes_del_inicio_se_rechaza() {
            assertThatThrownBy(() -> a("2026-12-01T11:59:59Z").registrar(TURNO_ID, "ausente", Rol.RECEPCION,
                    USUARIO, null))
                    .isInstanceOf(OperacionNoPermitidaException.class);
        }

        @Test
        void justo_al_inicio_se_acepta() {
            a("2026-12-01T12:00:00Z").registrar(TURNO_ID, "asistio", Rol.RECEPCION, USUARIO, null);

            verify(turnos).cambiarEstado(TURNO_ID, EstadoTurno.ASISTIO, Actor.usuario(USUARIO));
        }

        @Test
        void ausente_con_aviso_se_acepta_el_dia_anterior() {
            a("2026-11-30T15:00:00Z").registrar(TURNO_ID, "ausente_con_aviso", Rol.RECEPCION, USUARIO, null);

            verify(turnos).cambiarEstado(TURNO_ID, EstadoTurno.AUSENTE_CON_AVISO, Actor.usuario(USUARIO));
        }
    }

    @Test
    void devuelve_el_turno_que_devuelve_el_servicio() {
        assertThat(despuesDelInicio().registrar(TURNO_ID, "asistio", Rol.RECEPCION, USUARIO, null)).isSameAs(turno);
    }
}

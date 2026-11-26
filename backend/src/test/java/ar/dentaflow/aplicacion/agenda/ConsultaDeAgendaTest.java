package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

/** Reglas de la consulta de agenda (spec agenda-interna; design D1, D2). */
class ConsultaDeAgendaTest {

    private static final LocalDate DIA = LocalDate.parse("2026-12-01");

    private LecturaDeAgenda lectura;
    private ConsultaDeAgenda consulta;

    @BeforeEach
    void preparar() {
        lectura = mock(LecturaDeAgenda.class);
        when(lectura.buscar(any())).thenReturn(List.of());
        consulta = new ConsultaDeAgenda(lectura);
    }

    private CriterioDeAgenda criterioUsado() {
        ArgumentCaptor<CriterioDeAgenda> criterio = ArgumentCaptor.forClass(CriterioDeAgenda.class);
        verify(lectura).buscar(criterio.capture());
        return criterio.getValue();
    }

    private PedidoDeAgenda pedido(LocalDate desde, LocalDate hasta) {
        return new PedidoDeAgenda(desde, hasta, null, null, null);
    }

    @Nested
    class Rango {

        @Test
        void un_dia_del_consultorio_va_de_las_00_a_las_24_hora_argentina_en_utc() {
            consulta.consultar(pedido(DIA, DIA), Rol.RECEPCION, null);

            CriterioDeAgenda criterio = criterioUsado();
            assertThat(criterio.desde()).isEqualTo(Instant.parse("2026-12-01T03:00:00Z"));
            assertThat(criterio.hasta()).isEqualTo(Instant.parse("2026-12-02T03:00:00Z"));
        }

        @Test
        void una_semana_termina_al_final_del_domingo() {
            consulta.consultar(pedido(LocalDate.parse("2026-11-30"), LocalDate.parse("2026-12-06")), Rol.RECEPCION,
                    null);

            CriterioDeAgenda criterio = criterioUsado();
            assertThat(criterio.desde()).isEqualTo(Instant.parse("2026-11-30T03:00:00Z"));
            assertThat(criterio.hasta()).isEqualTo(Instant.parse("2026-12-07T03:00:00Z"));
        }

        @Test
        void rango_invertido_se_rechaza_sin_consultar() {
            assertThatThrownBy(() -> consulta.consultar(pedido(LocalDate.parse("2026-12-05"), DIA), Rol.RECEPCION,
                    null))
                    .isInstanceOf(ValidacionDeDominioException.class)
                    .hasMessageContaining("anterior");
            verify(lectura, never()).buscar(any());
        }

        @Test
        void mas_de_siete_dias_se_rechaza() {
            assertThatThrownBy(() -> consulta.consultar(pedido(DIA, LocalDate.parse("2026-12-08")), Rol.RECEPCION,
                    null))
                    .isInstanceOf(ValidacionDeDominioException.class)
                    .hasMessageContaining("7 días");
            verify(lectura, never()).buscar(any());
        }

        @Test
        void siete_dias_justos_se_aceptan() {
            consulta.consultar(pedido(DIA, LocalDate.parse("2026-12-07")), Rol.RECEPCION, null);

            assertThat(criterioUsado().hasta()).isEqualTo(Instant.parse("2026-12-08T03:00:00Z"));
        }

        @Test
        void sin_fechas_se_rechaza() {
            assertThatThrownBy(() -> consulta.consultar(pedido(null, DIA), Rol.RECEPCION, null))
                    .isInstanceOf(ValidacionDeDominioException.class);
        }
    }

    @Nested
    class Estados {

        @Test
        void sin_filtro_de_estado_excluye_los_cancelados() {
            consulta.consultar(pedido(DIA, DIA), Rol.ADMINISTRADOR, null);

            assertThat(criterioUsado().estados())
                    .containsExactlyInAnyOrder(EstadoTurno.PENDIENTE, EstadoTurno.CONFIRMADO, EstadoTurno.ASISTIO,
                            EstadoTurno.AUSENTE, EstadoTurno.AUSENTE_CON_AVISO);
        }

        @Test
        void filtrar_por_cancelado_devuelve_solo_cancelados() {
            consulta.consultar(new PedidoDeAgenda(DIA, DIA, null, null, "cancelado"), Rol.ADMINISTRADOR, null);

            assertThat(criterioUsado().estados()).containsExactly(EstadoTurno.CANCELADO);
        }

        @Test
        void estado_desconocido_se_rechaza_sin_repetir_el_valor_recibido() {
            assertThatThrownBy(() -> consulta.consultar(new PedidoDeAgenda(DIA, DIA, null, null, "borrado"),
                    Rol.RECEPCION, null))
                    .isInstanceOf(ValidacionDeDominioException.class)
                    .hasMessageNotContaining("borrado");
            verify(lectura, never()).buscar(any());
        }
    }

    @Nested
    class Permisos {

        @Test
        void recepcion_sin_filtros_ve_a_todos_los_profesionales_y_sillones() {
            consulta.consultar(pedido(DIA, DIA), Rol.RECEPCION, null);

            CriterioDeAgenda criterio = criterioUsado();
            assertThat(criterio.profesionalId()).isNull();
            assertThat(criterio.sillonId()).isNull();
        }

        @Test
        void administrador_puede_filtrar_por_cualquier_profesional_y_sillon() {
            consulta.consultar(new PedidoDeAgenda(DIA, DIA, 11L, 2L, null), Rol.ADMINISTRADOR, null);

            CriterioDeAgenda criterio = criterioUsado();
            assertThat(criterio.profesionalId()).isEqualTo(11L);
            assertThat(criterio.sillonId()).isEqualTo(2L);
        }

        @Test
        void profesional_sin_filtro_ve_solo_lo_suyo() {
            consulta.consultar(pedido(DIA, DIA), Rol.PROFESIONAL, 10L);

            assertThat(criterioUsado().profesionalId()).isEqualTo(10L);
        }

        @Test
        void profesional_puede_pedir_explicitamente_lo_suyo() {
            consulta.consultar(new PedidoDeAgenda(DIA, DIA, 10L, 1L, null), Rol.PROFESIONAL, 10L);

            assertThat(criterioUsado().profesionalId()).isEqualTo(10L);
            assertThat(criterioUsado().sillonId()).isEqualTo(1L);
        }

        @Test
        void profesional_que_pide_la_agenda_de_otro_recibe_403() {
            assertThatThrownBy(() -> consulta.consultar(new PedidoDeAgenda(DIA, DIA, 11L, null, null),
                    Rol.PROFESIONAL, 10L))
                    .isInstanceOf(AccessDeniedException.class);
            verify(lectura, never()).buscar(any());
        }

        @Test
        void profesional_sin_vinculo_recibe_403() {
            assertThatThrownBy(() -> consulta.consultar(pedido(DIA, DIA), Rol.PROFESIONAL, null))
                    .isInstanceOf(AccessDeniedException.class);
            verify(lectura, never()).buscar(any());
        }
    }
}

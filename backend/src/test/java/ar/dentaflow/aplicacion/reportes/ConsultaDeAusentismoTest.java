package ar.dentaflow.aplicacion.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.reportes.RegistroAsistenciaParaReporte;
import ar.dentaflow.dominio.reportes.ReporteDeAusentismo;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.usuarios.Rol;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/** Spec panel-ausentismo — validación de rango/umbral, zona horaria y permisos por rol en {@link ConsultaDeAusentismo}. */
@ExtendWith(MockitoExtension.class)
class ConsultaDeAusentismoTest {

    @Mock
    private LecturaDeAusentismo lectura;

    @InjectMocks
    private ConsultaDeAusentismo consulta;

    @Test
    void convierte_fechas_en_hora_argentina_y_usa_umbral_por_defecto_2() {
        LocalDate desde = LocalDate.of(2031, 3, 1);
        LocalDate hasta = LocalDate.of(2031, 3, 31);
        when(lectura.buscarEvaluados(any(), any(), eq(null))).thenReturn(List.of(
                new RegistroAsistenciaParaReporte(
                        Instant.parse("2031-03-04T13:00:00Z"), EstadoTurno.AUSENTE, 1L, "Laura", "Gómez", 10L, "Ana", "López")));

        ReporteDeAusentismo reporte = consulta.consultar(
                new PedidoDeAusentismo(desde, hasta, null, null), Rol.ADMINISTRADOR, null);

        verify(lectura).buscarEvaluados(
                Instant.parse("2031-03-01T03:00:00Z"),
                Instant.parse("2031-04-01T03:00:00Z"),
                null);
        assertThat(reporte.minimoAusencias()).isEqualTo(2);
        assertThat(reporte.general().totalEvaluados()).isEqualTo(1);
    }

    @Test
    void recepcion_puede_consultar_todo_o_filtrar_un_profesional() {
        LocalDate dia = LocalDate.of(2031, 3, 4);
        when(lectura.buscarEvaluados(any(), any(), eq(7L))).thenReturn(List.of());

        ReporteDeAusentismo reporte = consulta.consultar(
                new PedidoDeAusentismo(dia, dia, 7L, 1), Rol.RECEPCION, null);

        verify(lectura).buscarEvaluados(
                Instant.parse("2031-03-04T03:00:00Z"),
                Instant.parse("2031-03-05T03:00:00Z"),
                7L);
        assertThat(reporte.profesionalId()).isEqualTo(7L);
        assertThat(reporte.minimoAusencias()).isEqualTo(1);
    }

    @Test
    void profesional_sin_filtro_consulta_solo_sus_propios_turnos() {
        LocalDate dia = LocalDate.of(2031, 3, 4);
        when(lectura.buscarEvaluados(any(), any(), eq(5L))).thenReturn(List.of());

        ReporteDeAusentismo reporte = consulta.consultar(
                new PedidoDeAusentismo(dia, dia, null, null), Rol.PROFESIONAL, 5L);

        verify(lectura).buscarEvaluados(any(), any(), eq(5L));
        assertThat(reporte.profesionalId()).isEqualTo(5L);
    }

    @Test
    void profesional_pidiendo_otro_profesional_recibe_403_sin_leer_base() {
        LocalDate dia = LocalDate.of(2031, 3, 4);

        assertThatThrownBy(() -> consulta.consultar(
                new PedidoDeAusentismo(dia, dia, 9L, null), Rol.PROFESIONAL, 5L))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(lectura);
    }

    @Test
    void profesional_no_vinculado_recibe_403_sin_leer_base() {
        LocalDate dia = LocalDate.of(2031, 3, 4);

        assertThatThrownBy(() -> consulta.consultar(
                new PedidoDeAusentismo(dia, dia, null, null), Rol.PROFESIONAL, null))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(lectura);
    }

    @Test
    void rechaza_fechas_faltantes_invertidas_o_rango_mayor_a_366_dias() {
        LocalDate inicio = LocalDate.of(2031, 1, 1);

        assertThatThrownBy(() -> consulta.consultar(
                new PedidoDeAusentismo(null, inicio, null, null), Rol.ADMINISTRADOR, null))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThatThrownBy(() -> consulta.consultar(
                new PedidoDeAusentismo(LocalDate.of(2031, 3, 10), LocalDate.of(2031, 3, 1), null, null),
                Rol.ADMINISTRADOR, null))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThatThrownBy(() -> consulta.consultar(
                new PedidoDeAusentismo(inicio, inicio.plusDays(366), null, null), Rol.ADMINISTRADOR, null))
                .isInstanceOf(ValidacionDeDominioException.class);
        verifyNoInteractions(lectura);
    }

    @Test
    void rechaza_minimo_de_ausencias_menor_a_1() {
        LocalDate dia = LocalDate.of(2031, 3, 4);

        assertThatThrownBy(() -> consulta.consultar(
                new PedidoDeAusentismo(dia, dia, null, 0), Rol.ADMINISTRADOR, null))
                .isInstanceOf(ValidacionDeDominioException.class);
        verifyNoInteractions(lectura);
    }
}

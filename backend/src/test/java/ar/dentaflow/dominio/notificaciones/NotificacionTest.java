package ar.dentaflow.dominio.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Ciclo de vida de una notificación (design D3). */
class NotificacionTest {

    private static final Instant T0 = Instant.parse("2026-12-01T12:00:00Z");
    private static final Instant T1 = Instant.parse("2026-12-01T12:00:05Z");

    private Notificacion nueva() {
        return Notificacion.nueva(42L, TipoNotificacion.CONFIRMACION, T0);
    }

    @Test
    void naceEnCursoSinIntentosPorEmail() {
        Notificacion n = nueva();

        assertThat(n.getTurnoId()).isEqualTo(42L);
        assertThat(n.getTipo()).isEqualTo(TipoNotificacion.CONFIRMACION);
        assertThat(n.getCanal()).isEqualTo(CanalNotificacion.EMAIL);
        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.EN_CURSO);
        assertThat(n.getIntentos()).isZero();
        assertThat(n.getCreadaEn()).isEqualTo(T0);
        assertThat(n.getActualizadaEn()).isEqualTo(T0);
        assertThat(n.getEnviadaEn()).isNull();
        assertThat(n.getMotivoFallo()).isNull();
    }

    @Test
    void exigeTurnoTipoEInstante() {
        assertThatThrownBy(() -> Notificacion.nueva(null, TipoNotificacion.CONFIRMACION, T0))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Notificacion.nueva(42L, null, T0))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Notificacion.nueva(42L, TipoNotificacion.CONFIRMACION, null))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void registrarIntentoSumaYActualiza() {
        Notificacion n = nueva();

        n.registrarIntento(T1);
        n.registrarIntento(T1);

        assertThat(n.getIntentos()).isEqualTo(2);
        assertThat(n.getActualizadaEn()).isEqualTo(T1);
    }

    @Test
    void marcarEnviadaFijaElInstanteDeEnvio() {
        Notificacion n = nueva();
        n.registrarIntento(T1);

        n.marcarEnviada(T1);

        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.ENVIADA);
        assertThat(n.getEnviadaEn()).isEqualTo(T1);
        assertThat(n.getMotivoFallo()).isNull();
    }

    @Test
    void marcarFallidaExigeMotivo() {
        Notificacion n = nueva();
        n.registrarIntento(T1);

        assertThatThrownBy(() -> n.marcarFallida(null, T1)).isInstanceOf(ValidacionDeDominioException.class);

        n.marcarFallida(MotivoFallo.CONEXION, T1);
        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.FALLIDA);
        assertThat(n.getMotivoFallo()).isEqualTo(MotivoFallo.CONEXION);
        assertThat(n.getEnviadaEn()).isNull();
    }

    @Test
    void omitirDejaCeroIntentos() {
        Notificacion n = nueva();

        n.omitir(T1);

        assertThat(n.getEstado()).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(n.getIntentos()).isZero();
    }

    @Test
    void unaEnviadaNoVuelveAFallidaNiSumaIntentos() {
        Notificacion n = nueva();
        n.registrarIntento(T1);
        n.marcarEnviada(T1);

        assertThatThrownBy(() -> n.marcarFallida(MotivoFallo.CONEXION, T1))
                .isInstanceOf(OperacionNoPermitidaException.class);
        assertThatThrownBy(() -> n.registrarIntento(T1)).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void unaOmitidaNoSeEnvia() {
        Notificacion n = nueva();
        n.omitir(T1);

        assertThatThrownBy(() -> n.marcarEnviada(T1)).isInstanceOf(OperacionNoPermitidaException.class);
    }

    @Test
    void losCodigosCoincidenConLaBase() {
        assertThat(TipoNotificacion.CONFIRMACION.codigo()).isEqualTo("confirmacion");
        assertThat(TipoNotificacion.RECORDATORIO.codigo()).isEqualTo("recordatorio");
        assertThat(CanalNotificacion.EMAIL.codigo()).isEqualTo("email");
        assertThat(EstadoNotificacion.EN_CURSO.codigo()).isEqualTo("en_curso");
        assertThat(EstadoNotificacion.desdeCodigo("omitida")).isEqualTo(EstadoNotificacion.OMITIDA);
        assertThat(MotivoFallo.desdeCodigo("destinatario")).isEqualTo(MotivoFallo.DESTINATARIO);
        assertThatThrownBy(() -> TipoNotificacion.desdeCodigo("reintento"))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void toStringNoTieneDatosDelPaciente() {
        assertThat(nueva().toString()).contains("turno=42").doesNotContain("@");
    }
}

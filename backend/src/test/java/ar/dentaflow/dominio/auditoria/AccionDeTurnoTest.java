package ar.dentaflow.dominio.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;

/** Spec "Acción de un catálogo cerrado". */
class AccionDeTurnoTest {

    @Test
    void cadaAccionTieneSuCodigo() {
        assertThat(AccionDeTurno.CREACION.codigo()).isEqualTo("creacion");
        assertThat(AccionDeTurno.CONFIRMACION.codigo()).isEqualTo("confirmacion");
        assertThat(AccionDeTurno.CANCELACION.codigo()).isEqualTo("cancelacion");
        assertThat(AccionDeTurno.REPROGRAMACION.codigo()).isEqualTo("reprogramacion");
        assertThat(AccionDeTurno.CAMBIO_ESTADO.codigo()).isEqualTo("cambio_estado");
        assertThat(AccionDeTurno.values()).hasSize(5);
    }

    @Test
    void seReconstruyeDesdeElCodigo() {
        assertThat(AccionDeTurno.desdeCodigo("cambio_estado")).isEqualTo(AccionDeTurno.CAMBIO_ESTADO);
        assertThat(AccionDeTurno.desdeCodigo("creacion")).isEqualTo(AccionDeTurno.CREACION);
    }

    @Test
    void unaAccionDesconocidaSeRechaza() {
        assertThatThrownBy(() -> AccionDeTurno.desdeCodigo("borrado"))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("borrado");
        assertThatThrownBy(() -> AccionDeTurno.desdeCodigo(null)).isInstanceOf(ValidacionDeDominioException.class);
    }
}

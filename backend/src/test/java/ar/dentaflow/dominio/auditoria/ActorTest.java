package ar.dentaflow.dominio.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;

/** Spec "Actor identificado": un solo tipo de actor y el id solo para el personal. */
class ActorTest {

    @Test
    void elActorUsuarioLlevaSuId() {
        Actor actor = Actor.usuario(7L);

        assertThat(actor.tipo()).isEqualTo(TipoDeActor.USUARIO);
        assertThat(actor.usuarioId()).isEqualTo(7L);
    }

    @Test
    void elActorUsuarioSinIdSeRechaza() {
        assertThatThrownBy(() -> Actor.usuario(null))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("usuario");
    }

    @Test
    void elActorUsuarioConIdNoPositivoSeRechaza() {
        assertThatThrownBy(() -> Actor.usuario(0L)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Actor.usuario(-3L)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void elPacienteNoTieneIdDeUsuario() {
        Actor actor = Actor.paciente();

        assertThat(actor.tipo()).isEqualTo(TipoDeActor.PACIENTE);
        assertThat(actor.usuarioId()).isNull();
    }

    @Test
    void elSistemaNoTieneIdDeUsuario() {
        Actor actor = Actor.sistema();

        assertThat(actor.tipo()).isEqualTo(TipoDeActor.SISTEMA);
        assertThat(actor.usuarioId()).isNull();
    }

    @Test
    void noSePuedeArmarUnActorIncoherente() {
        assertThatThrownBy(() -> new Actor(TipoDeActor.PACIENTE, 7L)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> new Actor(TipoDeActor.SISTEMA, 7L)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> new Actor(null, null)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void cadaTipoTieneSuCodigoPersistido() {
        assertThat(TipoDeActor.USUARIO.codigo()).isEqualTo("usuario");
        assertThat(TipoDeActor.PACIENTE.codigo()).isEqualTo("paciente");
        assertThat(TipoDeActor.SISTEMA.codigo()).isEqualTo("sistema");
        assertThat(TipoDeActor.desdeCodigo("paciente")).isEqualTo(TipoDeActor.PACIENTE);
        assertThatThrownBy(() -> TipoDeActor.desdeCodigo("robot")).isInstanceOf(ValidacionDeDominioException.class);
    }
}

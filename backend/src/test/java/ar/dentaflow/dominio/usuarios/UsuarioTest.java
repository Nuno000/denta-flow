package ar.dentaflow.dominio.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec gestion-usuarios — invariantes del usuario (design D4). */
class UsuarioTest {

    private static final String HASH = "$2a$10$hashDePruebaQueNoEsUnaContrasenaReal";

    @Test
    void nuevoQuedaActivoConElEmailNormalizado() {
        Usuario usuario = Usuario.nuevo("  Recepcion@Example.com ", HASH, Rol.RECEPCION, null);

        assertThat(usuario.isActivo()).isTrue();
        assertThat(usuario.getEmail()).isEqualTo("recepcion@example.com");
        assertThat(usuario.getRol()).isEqualTo(Rol.RECEPCION);
        assertThat(usuario.getCredencialHash()).isEqualTo(HASH);
        assertThat(usuario.getProfesionalId()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "recepcion-sin-arroba", "dos@@example.com", "sin-dominio@"})
    void rechazaEmailsInvalidos(String email) {
        assertThatThrownBy(() -> Usuario.nuevo(email, HASH, Rol.RECEPCION, null))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rechazaEmailDeMasDe254Caracteres() {
        String email = "a".repeat(243) + "@example.com";

        assertThatThrownBy(() -> Usuario.nuevo(email, HASH, Rol.RECEPCION, null))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("254");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rechazaHashEnBlanco(String hash) {
        assertThatThrownBy(() -> Usuario.nuevo("a@example.com", hash, Rol.RECEPCION, null))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("credencial");
    }

    @Test
    void rechazaRolNulo() {
        assertThatThrownBy(() -> Usuario.nuevo("a@example.com", HASH, null, null))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("rol es obligatorio");
    }

    @Test
    void rolProfesionalExigeProfesionalVinculado() {
        assertThatThrownBy(() -> Usuario.nuevo("p@example.com", HASH, Rol.PROFESIONAL, null))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("requiere un profesional vinculado");

        Usuario profesional = Usuario.nuevo("p@example.com", HASH, Rol.PROFESIONAL, 7L);
        assertThat(profesional.getProfesionalId()).isEqualTo(7L);
    }

    @Test
    void losOtrosRolesNoSeVinculanAUnProfesional() {
        assertThatThrownBy(() -> Usuario.nuevo("r@example.com", HASH, Rol.RECEPCION, 7L))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("Solo el rol profesional");
        assertThatThrownBy(() -> Usuario.nuevo("a@example.com", HASH, Rol.ADMINISTRADOR, 7L))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("Solo el rol profesional");
    }

    @Test
    void cambiarRolAplicaLaMismaReglaYNoCambiaNadaSiFalla() {
        Usuario usuario = Usuario.nuevo("r@example.com", HASH, Rol.RECEPCION, null);

        assertThatThrownBy(() -> usuario.cambiarRol(Rol.PROFESIONAL, null))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(usuario.getRol()).isEqualTo(Rol.RECEPCION);
        assertThat(usuario.getProfesionalId()).isNull();

        usuario.cambiarRol(Rol.PROFESIONAL, 3L);
        assertThat(usuario.getRol()).isEqualTo(Rol.PROFESIONAL);
        assertThat(usuario.getProfesionalId()).isEqualTo(3L);
    }

    @Test
    void deProfesionalARecepcionLimpiaElVinculo() {
        Usuario usuario = Usuario.nuevo("p@example.com", HASH, Rol.PROFESIONAL, 3L);

        usuario.cambiarRol(Rol.RECEPCION, null);

        assertThat(usuario.getRol()).isEqualTo(Rol.RECEPCION);
        assertThat(usuario.getProfesionalId()).isNull();
    }

    @Test
    void desactivarYActivarSonIdempotentesYNoTocanOtrosDatos() {
        Usuario usuario = Usuario.nuevo("r@example.com", HASH, Rol.RECEPCION, null);

        usuario.desactivar();
        usuario.desactivar();
        assertThat(usuario.isActivo()).isFalse();
        assertThat(usuario.getEmail()).isEqualTo("r@example.com");
        assertThat(usuario.getRol()).isEqualTo(Rol.RECEPCION);

        usuario.activar();
        usuario.activar();
        assertThat(usuario.isActivo()).isTrue();
    }

    @Test
    void cambiarCredencialReemplazaElHashYRechazaUnoEnBlanco() {
        Usuario usuario = Usuario.nuevo("r@example.com", HASH, Rol.RECEPCION, null);

        assertThatThrownBy(() -> usuario.cambiarCredencial(" "))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(usuario.getCredencialHash()).isEqualTo(HASH);

        usuario.cambiarCredencial("$2a$10$otroHash");
        assertThat(usuario.getCredencialHash()).isEqualTo("$2a$10$otroHash");
    }

    @Test
    void igualdadPorId() {
        Usuario uno = ar.dentaflow.dominio.recursos.AyudanteDeIds.conId(
                Usuario.nuevo("a@example.com", HASH, Rol.RECEPCION, null), 1L);
        Usuario mismoId = ar.dentaflow.dominio.recursos.AyudanteDeIds.conId(
                Usuario.nuevo("b@example.com", HASH, Rol.ADMINISTRADOR, null), 1L);
        Usuario sinId = Usuario.nuevo("a@example.com", HASH, Rol.RECEPCION, null);

        assertThat(uno).isEqualTo(mismoId).hasSameHashCodeAs(mismoId);
        assertThat(sinId).isNotEqualTo(Usuario.nuevo("a@example.com", HASH, Rol.RECEPCION, null));
        assertThat(sinId).isEqualTo(sinId);
    }

    @Test
    void elRolSeLeeYSeEscribePorCodigo() {
        assertThat(Rol.ADMINISTRADOR.codigo()).isEqualTo("administrador");
        assertThat(Rol.desdeCodigo("recepcion")).isEqualTo(Rol.RECEPCION);
        assertThat(Rol.desdeCodigo("profesional")).isEqualTo(Rol.PROFESIONAL);
        assertThatThrownBy(() -> Rol.desdeCodigo("superusuario"))
                .isInstanceOf(ValidacionDeDominioException.class)
                .hasMessageContaining("administrador, recepcion, profesional");
    }
}

package ar.dentaflow.infraestructura.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Spec autenticacion-personal — "Credenciales almacenadas con hash". */
class CodificadorBCryptTest {

    private final CodificadorBCrypt codificador = new CodificadorBCrypt();

    @Test
    void elHashNoEsLaContrasenaYCadaHashLlevaSuPropiaSal() {
        String hash1 = codificador.codificar("contrasena-de-prueba");
        String hash2 = codificador.codificar("contrasena-de-prueba");

        assertThat(hash1).isNotEqualTo("contrasena-de-prueba").startsWith("$2");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void coincideSoloConLaContrasenaCorrecta() {
        String hash = codificador.codificar("contrasena-de-prueba");

        assertThat(codificador.coincide("contrasena-de-prueba", hash)).isTrue();
        assertThat(codificador.coincide("otra-contrasena", hash)).isFalse();
    }
}

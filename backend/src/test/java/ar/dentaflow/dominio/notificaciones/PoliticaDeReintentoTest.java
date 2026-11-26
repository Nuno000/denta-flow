package ar.dentaflow.dominio.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Spec notificaciones-email — "Reintento ante fallas transitorias" (design D3). */
class PoliticaDeReintentoTest {

    private final PoliticaDeReintento politica = new PoliticaDeReintento();

    @Test
    void elMaximoEsTresIntentos() {
        assertThat(PoliticaDeReintento.INTENTOS_MAXIMOS).isEqualTo(3);
    }

    @Test
    void unaFallaDeConexionSeReintentaHastaCompletarTres() {
        assertThat(politica.debeReintentar(1, MotivoFallo.CONEXION)).isTrue();
        assertThat(politica.debeReintentar(2, MotivoFallo.CONEXION)).isTrue();
        assertThat(politica.debeReintentar(3, MotivoFallo.CONEXION)).isFalse();
    }

    @Test
    void unaFallaDesconocidaSeTrataComoTransitoria() {
        assertThat(politica.debeReintentar(1, MotivoFallo.DESCONOCIDO)).isTrue();
        assertThat(politica.debeReintentar(3, MotivoFallo.DESCONOCIDO)).isFalse();
    }

    @Test
    void lasFallasPermanentesNuncaSeReintentan() {
        assertThat(politica.debeReintentar(1, MotivoFallo.AUTENTICACION)).isFalse();
        assertThat(politica.debeReintentar(1, MotivoFallo.DESTINATARIO)).isFalse();
    }
}

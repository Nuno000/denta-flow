package ar.dentaflow.dominio.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Spec pacientes — datos obligatorios, formato del email, teléfono y consentimiento (design D3). */
class PacienteTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T12:00:00Z");

    private static Paciente ana() {
        return Paciente.nuevo("30.123.456", "Ana", "Ejemplo", "+54 11 5555-0000", "Ana@Example.COM", AHORA);
    }

    @Test
    void altaConLosCincoDatosObligatoriosNormalizados() {
        Paciente paciente = ana();

        assertThat(paciente.getDni()).isEqualTo("30123456");
        assertThat(paciente.getNombre()).isEqualTo("Ana");
        assertThat(paciente.getApellido()).isEqualTo("Ejemplo");
        assertThat(paciente.getTelefono()).isEqualTo("+541155550000");
        assertThat(paciente.getEmail()).isEqualTo("ana@example.com");
        assertThat(paciente.getCreadoEn()).isEqualTo(AHORA);
        assertThat(paciente.getConsentimientoPrivacidadEn()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rechazaCadaDatoObligatorioNuloOEnBlanco(String vacio) {
        assertThatThrownBy(() -> Paciente.nuevo(vacio, "Ana", "Ejemplo", "1155550000", "a@example.com", AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("DNI");
        assertThatThrownBy(() -> Paciente.nuevo("30123456", vacio, "Ejemplo", "1155550000", "a@example.com", AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("nombre");
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", vacio, "1155550000", "a@example.com", AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("apellido");
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", "Ejemplo", vacio, "a@example.com", AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("teléfono");
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", "Ejemplo", "1155550000", vacio, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@", "ana example.com", "ana@example", "@example.com", "a@@example.com"})
    void rechazaUnEmailConFormatoInvalido(String email) {
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", "Ejemplo", "1155550000", email, AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"5555", "abc12345678", "+1234567", "1234567890123456", "11 5555 00 0x"})
    void rechazaUnTelefonoInvalido(String telefono) {
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", "Ejemplo", telefono, "a@example.com", AHORA))
                .isInstanceOf(ValidacionDeDominioException.class).hasMessageContaining("teléfono");
    }

    @Test
    void aceptaTelefonosEnLosLimitesDeLargo() {
        assertThat(Paciente.nuevo("30123456", "Ana", "Ejemplo", "12345678", "a@example.com", AHORA).getTelefono())
                .isEqualTo("12345678");
        assertThat(Paciente.nuevo("30123456", "Ana", "Ejemplo", "+123456789012345", "a@example.com", AHORA)
                .getTelefono()).isEqualTo("+123456789012345");
    }

    @Test
    void rechazaLosLargosMaximosSuperados() {
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "n".repeat(101), "Ejemplo", "1155550000",
                "a@example.com", AHORA)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", "a".repeat(101), "1155550000",
                "a@example.com", AHORA)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> Paciente.nuevo("30123456", "Ana", "Ejemplo", "1155550000",
                "a".repeat(250) + "@e.com", AHORA)).isInstanceOf(ValidacionDeDominioException.class);
        assertThat(Paciente.nuevo("30123456", "n".repeat(100), "a".repeat(100), "1155550000", "a@example.com", AHORA)
                .getNombre()).hasSize(100);
    }

    @Test
    void actualizarContactoReemplazaLosCuatroDatosConLasMismasValidaciones() {
        Paciente paciente = ana();

        paciente.actualizarContacto("Anita", "Nueva", "11 4444-0000", "NUEVA@example.com");

        assertThat(paciente.getNombre()).isEqualTo("Anita");
        assertThat(paciente.getApellido()).isEqualTo("Nueva");
        assertThat(paciente.getTelefono()).isEqualTo("1144440000");
        assertThat(paciente.getEmail()).isEqualTo("nueva@example.com");
        assertThat(paciente.getDni()).isEqualTo("30123456");
    }

    @Test
    void actualizarContactoInvalidoNoCambiaNada() {
        Paciente paciente = ana();

        assertThatThrownBy(() -> paciente.actualizarContacto("Anita", "Nueva", "5555", "nueva@example.com"))
                .isInstanceOf(ValidacionDeDominioException.class);

        assertThat(paciente.getNombre()).isEqualTo("Ana");
        assertThat(paciente.getEmail()).isEqualTo("ana@example.com");
    }

    @Test
    void registrarConsentimientoGuardaElInstanteYUnaSegundaAceptacionLoReemplaza() {
        Paciente paciente = ana();
        Instant primera = Instant.parse("2026-12-01T12:05:00Z");
        Instant segunda = Instant.parse("2026-12-05T09:00:00Z");

        paciente.registrarConsentimiento(primera);
        assertThat(paciente.getConsentimientoPrivacidadEn()).isEqualTo(primera);

        paciente.registrarConsentimiento(segunda);
        assertThat(paciente.getConsentimientoPrivacidadEn()).isEqualTo(segunda);
    }

    @Test
    void registrarConsentimientoNuloEsRechazado() {
        assertThatThrownBy(() -> ana().registrarConsentimiento(null))
                .isInstanceOf(ValidacionDeDominioException.class);
    }
}

package ar.dentaflow.dominio.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Spec privacidad-datos-personales (C-15), "Anonimización del paciente" y "Sin emails a pacientes anonimizados",
 * en el dominio puro (design D4, revisado: el DNI queda vacío).
 */
class PacienteAnonimizarTest {

    private static final Instant ALTA = Instant.parse("2026-11-01T12:00:00Z");
    private static final Instant CONSENTIMIENTO = Instant.parse("2026-11-01T12:00:05Z");
    private static final Instant BAJA = Instant.parse("2026-12-01T15:30:00Z");

    private static Paciente pacienteGuardado(long id) throws ReflectiveOperationException {
        Paciente paciente = Paciente.nuevo("30111222", "Ana", "Prueba", "1155550000", "ana.prueba@example.com", ALTA);
        paciente.registrarConsentimiento(CONSENTIMIENTO);
        Field campoId = Paciente.class.getDeclaredField("id");
        campoId.setAccessible(true);
        campoId.set(paciente, id);
        return paciente;
    }

    @Test
    void reemplazaLosDatosPersonalesYRegistraQuienYCuando() throws Exception {
        Paciente paciente = pacienteGuardado(42L);

        paciente.anonimizar(7L, BAJA);

        assertThat(paciente.getDni()).isNull();
        assertThat(paciente.getNombre()).isEqualTo("Paciente");
        assertThat(paciente.getApellido()).isEqualTo("Anonimizado");
        assertThat(paciente.getTelefono()).isEqualTo("00000000");
        assertThat(paciente.getEmail()).isEqualTo("anonimizado-42@dentaflow.invalid");
        assertThat(paciente.getAnonimizadoEn()).isEqualTo(BAJA);
        assertThat(paciente.getAnonimizadoPor()).isEqualTo(7L);
        assertThat(paciente.estaAnonimizado()).isTrue();
    }

    @Test
    void conservaElConsentimientoYLaFechaDeAlta() throws Exception {
        Paciente paciente = pacienteGuardado(42L);

        paciente.anonimizar(7L, BAJA);

        assertThat(paciente.getConsentimientoPrivacidadEn()).isEqualTo(CONSENTIMIENTO);
        assertThat(paciente.getCreadoEn()).isEqualTo(ALTA);
    }

    @Test
    void anonimizarDosVecesNoCambiaNada() throws Exception {
        Paciente paciente = pacienteGuardado(42L);
        paciente.anonimizar(7L, BAJA);

        paciente.anonimizar(9L, BAJA.plusSeconds(3600));

        assertThat(paciente.getAnonimizadoPor()).isEqualTo(7L);
        assertThat(paciente.getAnonimizadoEn()).isEqualTo(BAJA);
    }

    @Test
    void soloRecibeEmailsSiNoEstaAnonimizado() throws Exception {
        Paciente paciente = pacienteGuardado(42L);
        assertThat(paciente.puedeRecibirEmails()).isTrue();

        paciente.anonimizar(7L, BAJA);

        assertThat(paciente.puedeRecibirEmails()).isFalse();
    }

    @Test
    void rechazaDatosFaltantesSinCambiarNada() throws Exception {
        Paciente paciente = pacienteGuardado(42L);

        assertThatThrownBy(() -> paciente.anonimizar(0L, BAJA)).isInstanceOf(ValidacionDeDominioException.class);
        assertThatThrownBy(() -> paciente.anonimizar(7L, null)).isInstanceOf(ValidacionDeDominioException.class);
        assertThat(paciente.getDni()).isEqualTo("30111222");
        assertThat(paciente.estaAnonimizado()).isFalse();
    }

    @Test
    void unPacienteSinGuardarNoSePuedeAnonimizar() {
        Paciente nuevo = Paciente.nuevo("30111222", "Ana", "Prueba", "1155550000", "ana.prueba@example.com", ALTA);

        assertThatThrownBy(() -> nuevo.anonimizar(7L, BAJA)).isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void unAnonimizadoNoAceptaDatosDeContactoNuevos() throws Exception {
        Paciente paciente = pacienteGuardado(42L);
        paciente.anonimizar(7L, BAJA);

        assertThatThrownBy(() -> paciente.actualizarContacto("Ana", "Prueba", "1155550000", "ana@example.com"))
                .isInstanceOf(ValidacionDeDominioException.class);
        assertThat(paciente.getNombre()).isEqualTo("Paciente");
    }
}

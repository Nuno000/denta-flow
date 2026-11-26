package ar.dentaflow.aplicacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

/** Spec pacientes — "DNI único con deduplicación", "Actualización de datos según el origen", "Fecha de consentimiento". */
class ServicioDePacientesTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");

    private PacienteRepositorio pacientes;
    private ServicioDePacientes servicio;

    @BeforeEach
    void preparar() {
        pacientes = mock(PacienteRepositorio.class);
        PlatformTransactionManager transacciones = mock(PlatformTransactionManager.class);
        when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(pacientes.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        servicio = new ServicioDePacientes(pacientes, Clock.fixed(AHORA, ZoneOffset.UTC), transacciones);
    }

    private static DatosPaciente datos(String dni, String nombre, String email, Instant consentimiento) {
        return new DatosPaciente(dni, nombre, "Ejemplo", "1155550000", email, consentimiento);
    }

    private static Paciente existente() {
        return Paciente.nuevo("30123456", "Ana", "Original", "1100000000", "original@example.com",
                Instant.parse("2026-11-01T10:00:00Z"));
    }

    @Test
    void unDniNuevoCreaAlPacienteConLaFechaDelReloj() {
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.empty());

        Paciente creado = servicio.registrarDesdeReservaOnline(datos("30.123.456", "Ana", "ana@example.com", null));

        assertThat(creado.getDni()).isEqualTo("30123456");
        assertThat(creado.getCreadoEn()).isEqualTo(AHORA);
        assertThat(creado.getConsentimientoPrivacidadEn()).isNull();
        verify(pacientes).guardar(creado);
    }

    @Test
    void unDniNuevoDesdeRecepcionTambienCreaAlPaciente() {
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.empty());

        Paciente creado = servicio.registrarDesdeRecepcion(datos("30123456", "Ana", "ana@example.com", null));

        assertThat(creado.getNombre()).isEqualTo("Ana");
        verify(pacientes).guardar(creado);
    }

    @Test
    void unDniExistenteConOtroFormatoDevuelveAlMismoPacienteSinAltaNueva() {
        Paciente actual = existente();
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.of(actual));

        Paciente devuelto = servicio.registrarDesdeReservaOnline(datos("30-123-456", "Otra", "otra@example.com", null));

        assertThat(devuelto).isSameAs(actual);
        verify(pacientes, never()).guardar(any());
    }

    @Test
    void onlineNoCambiaElContactoDelPacienteExistente() {
        Paciente actual = existente();
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.of(actual));

        servicio.registrarDesdeReservaOnline(datos("30123456", "Intruso", "intruso@example.com", null));

        assertThat(actual.getNombre()).isEqualTo("Ana");
        assertThat(actual.getApellido()).isEqualTo("Original");
        assertThat(actual.getTelefono()).isEqualTo("1100000000");
        assertThat(actual.getEmail()).isEqualTo("original@example.com");
    }

    @Test
    void recepcionReemplazaElContactoDelPacienteExistente() {
        Paciente actual = existente();
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.of(actual));

        Paciente devuelto = servicio.registrarDesdeRecepcion(datos("30123456", "Anita", "nuevo@example.com", null));

        assertThat(devuelto).isSameAs(actual);
        assertThat(actual.getNombre()).isEqualTo("Anita");
        assertThat(actual.getApellido()).isEqualTo("Ejemplo");
        assertThat(actual.getTelefono()).isEqualTo("1155550000");
        assertThat(actual.getEmail()).isEqualTo("nuevo@example.com");
        verify(pacientes).guardar(actual);
    }

    @Test
    void elConsentimientoPresenteReemplazaLaFechaEnAmbosOrigenes() {
        Instant primera = Instant.parse("2026-12-01T10:30:00Z");
        Instant segunda = Instant.parse("2026-12-01T10:45:00Z");
        Paciente actual = existente();
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.of(actual));

        servicio.registrarDesdeReservaOnline(datos("30123456", "Ana", "ana@example.com", primera));
        assertThat(actual.getConsentimientoPrivacidadEn()).isEqualTo(primera);
        assertThat(actual.getEmail()).isEqualTo("original@example.com");

        servicio.registrarDesdeRecepcion(datos("30123456", "Ana", "ana@example.com", segunda));
        assertThat(actual.getConsentimientoPrivacidadEn()).isEqualTo(segunda);
    }

    @Test
    void sinConsentimientoNoTocaLaFechaExistente() {
        Paciente actual = existente();
        Instant previa = Instant.parse("2026-11-15T10:00:00Z");
        actual.registrarConsentimiento(previa);
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.of(actual));

        servicio.registrarDesdeRecepcion(datos("30123456", "Ana", "ana@example.com", null));

        assertThat(actual.getConsentimientoPrivacidadEn()).isEqualTo(previa);
    }

    @Test
    void unPacienteNuevoConConsentimientoLoGuarda() {
        Instant consentimiento = Instant.parse("2026-12-01T10:30:00Z");
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.empty());

        Paciente creado = servicio.registrarDesdeReservaOnline(
                datos("30123456", "Ana", "ana@example.com", consentimiento));

        assertThat(creado.getConsentimientoPrivacidadEn()).isEqualTo(consentimiento);
    }

    @Test
    void unDniInvalidoSeRechazaSinConsultarLaBase() {
        assertThatThrownBy(() -> servicio.registrarDesdeReservaOnline(datos("123", "Ana", "ana@example.com", null)))
                .isInstanceOf(ValidacionDeDominioException.class);

        verify(pacientes, never()).buscarPorDni(any());
        verify(pacientes, never()).guardar(any());
    }

    @Test
    void siFallaElAltaPorDniDuplicadoReleeYDevuelveAlGanador() {
        Paciente ganador = existente();
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.empty()).thenReturn(Optional.of(ganador));
        when(pacientes.guardar(any())).thenThrow(new DataIntegrityViolationException("uk_paciente_dni"));

        Paciente devuelto = servicio.registrarDesdeReservaOnline(datos("30123456", "Ana", "ana@example.com", null));

        assertThat(devuelto).isSameAs(ganador);
        verify(pacientes, times(2)).buscarPorDni("30123456");
    }

    @Test
    void siVuelveAFallarSePropaga() {
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.empty());
        when(pacientes.guardar(any())).thenThrow(new DataIntegrityViolationException("uk_paciente_dni"));

        assertThatThrownBy(() -> servicio.registrarDesdeReservaOnline(
                datos("30123456", "Ana", "ana@example.com", null)))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(pacientes, times(2)).guardar(any());
    }

    @Test
    void buscarPorDniNormalizaAntesDeBuscar() {
        Paciente actual = existente();
        when(pacientes.buscarPorDni("30123456")).thenReturn(Optional.of(actual));

        assertThat(servicio.buscarPorDni("30.123.456")).containsSame(actual);
        assertThat(servicio.buscarPorDni("30 123 456")).containsSame(actual);
    }
}

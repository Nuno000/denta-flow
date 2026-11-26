package ar.dentaflow.aplicacion.pacientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.pacientes.TurnosDelPaciente;
import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec privacidad-datos-personales (C-15), "Anonimización del paciente" (design D4). */
class ServicioDeAnonimizacionTest {

    private static final Instant AHORA = Instant.parse("2026-12-01T15:30:00Z");

    private PacienteRepositorio pacientes;
    private TurnosDelPaciente turnos;
    private ServicioDeAnonimizacion servicio;

    @BeforeEach
    void preparar() {
        pacientes = mock(PacienteRepositorio.class);
        turnos = mock(TurnosDelPaciente.class);
        when(pacientes.guardar(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        servicio = new ServicioDeAnonimizacion(pacientes, turnos, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    private static Paciente paciente(long id) throws ReflectiveOperationException {
        Paciente paciente = Paciente.nuevo("30111222", "Ana", "Prueba", "1155550000", "ana@example.com",
                Instant.parse("2026-11-01T12:00:00Z"));
        Field campo = Paciente.class.getDeclaredField("id");
        campo.setAccessible(true);
        campo.set(paciente, id);
        return paciente;
    }

    @Test
    void sinTurnosFuturosActivosAnonimizaConElRelojYLoGuarda() throws Exception {
        Paciente paciente = paciente(42L);
        when(pacientes.buscarPorIdParaActualizar(42L)).thenReturn(Optional.of(paciente));
        when(turnos.hayActivosFuturos(42L, AHORA)).thenReturn(false);

        Paciente resultado = servicio.anonimizar(42L, 7L);

        assertThat(resultado.estaAnonimizado()).isTrue();
        assertThat(resultado.getAnonimizadoEn()).isEqualTo(AHORA);
        assertThat(resultado.getAnonimizadoPor()).isEqualTo(7L);
        verify(pacientes).guardar(paciente);
    }

    @Test
    void conTurnosFuturosActivosEsConflictoYNoCambiaNada() throws Exception {
        Paciente paciente = paciente(42L);
        when(pacientes.buscarPorIdParaActualizar(42L)).thenReturn(Optional.of(paciente));
        when(turnos.hayActivosFuturos(42L, AHORA)).thenReturn(true);

        assertThatThrownBy(() -> servicio.anonimizar(42L, 7L))
                .isInstanceOf(ConflictoException.class)
                .hasMessage(ServicioDeAnonimizacion.TIENE_TURNOS_ACTIVOS);

        assertThat(paciente.estaAnonimizado()).isFalse();
        verify(pacientes, never()).guardar(any());
    }

    @Test
    void unPacienteInexistenteNoSeEncuentra() {
        when(pacientes.buscarPorIdParaActualizar(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.anonimizar(99L, 7L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void unPacienteYaAnonimizadoSeDevuelveSinCambios() throws Exception {
        Paciente paciente = paciente(42L);
        paciente.anonimizar(5L, Instant.parse("2026-11-20T10:00:00Z"));
        when(pacientes.buscarPorIdParaActualizar(42L)).thenReturn(Optional.of(paciente));

        Paciente resultado = servicio.anonimizar(42L, 7L);

        assertThat(resultado.getAnonimizadoPor()).isEqualTo(5L);
        verify(turnos, never()).hayActivosFuturos(anyLong(), any());
        verify(pacientes, never()).guardar(any());
    }
}

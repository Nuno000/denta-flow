package ar.dentaflow.aplicacion.listaespera;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEspera;
import ar.dentaflow.dominio.listaespera.ListaEsperaRepositorio;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServicioDeListaDeEsperaTest {

    @Mock
    private ListaEsperaRepositorio listaEspera;
    @Mock
    private PacienteRepositorio pacientes;
    @Mock
    private PrestacionRepositorio prestaciones;
    @Mock
    private ProfesionalRepositorio profesionales;

    private ServicioDeListaDeEspera servicio;
    private static final Instant AHORA = Instant.parse("2026-12-01T15:00:00Z");

    @BeforeEach
    void preparar() {
        servicio = new ServicioDeListaDeEspera(
                listaEspera, pacientes, prestaciones, profesionales,
                Clock.fixed(AHORA, ZonaHorariaConsultorio.ZONA));
    }

    @Test
    void registraEntradaValidaSinProfesional() {
        Paciente paciente = Paciente.nuevo("30123456", "Ana", "Gomez", "1122334455", "ana@example.com", AHORA);
        Prestacion prestacion = Prestacion.nueva("Limpieza", 30);
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(prestaciones.buscarPorId(2L)).thenReturn(Optional.of(prestacion));
        when(listaEspera.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        ListaEsperaDetalle detalle = servicio.registrar(new SolicitudListaEspera(
                1L, 2L, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10),
                null, null), 99L);

        assertThat(detalle.estado()).isEqualTo("activa");
        assertThat(detalle.pacienteNombre()).isEqualTo("Ana Gomez");
        assertThat(detalle.prestacionNombre()).isEqualTo("Limpieza");
    }

    @Test
    void rechazaSiFechaHastaEsAnteriorAHoy() {
        Paciente paciente = Paciente.nuevo("30123456", "Ana", "Gomez", "1122334455", "ana@example.com", AHORA);
        Prestacion prestacion = Prestacion.nueva("Limpieza", 30);
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(prestaciones.buscarPorId(2L)).thenReturn(Optional.of(prestacion));

        assertThatThrownBy(() -> servicio.registrar(new SolicitudListaEspera(
                1L, 2L, null,
                LocalDate.of(2026, 11, 25), LocalDate.of(2026, 11, 30),
                null, null), 99L))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void rechazaProfesionalQueNoRealizaLaPrestacion() {
        Paciente paciente = Paciente.nuevo("30123456", "Ana", "Gomez", "1122334455", "ana@example.com", AHORA);
        Prestacion prestacion = Prestacion.nueva("Limpieza", 30);
        Profesional profesional = Profesional.nuevo("Maria", "Lopez", "MP1", "maria@example.com");
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(prestaciones.buscarPorId(2L)).thenReturn(Optional.of(prestacion));
        when(profesionales.buscarPorId(3L)).thenReturn(Optional.of(profesional));

        assertThatThrownBy(() -> servicio.registrar(new SolicitudListaEspera(
                1L, 2L, 3L,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10),
                null, null), 99L))
                .isInstanceOf(ValidacionDeDominioException.class);
    }

    @Test
    void darDeBajaCambiaActivaAVencidaYRechazaNoExistente() {
        ListaEspera entrada = ListaEspera.nueva(1L, 2L, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 5), null, null, 99L, AHORA);
        when(listaEspera.buscarPorId(5L)).thenReturn(Optional.of(entrada));
        when(listaEspera.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.darDeBaja(5L);
        assertThat(entrada.getEstado()).isEqualTo(EstadoListaEspera.VENCIDA);

        when(listaEspera.buscarPorId(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servicio.darDeBaja(999L))
                .isInstanceOf(RecursoNoEncontradoException.class);

        entrada.marcarVencida();
        assertThatThrownBy(() -> servicio.darDeBaja(5L))
                .isInstanceOf(ConflictoException.class);
    }
}

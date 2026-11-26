package ar.dentaflow.aplicacion.turnos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.pacientes.DatosPaciente;
import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.RecursoOcupado;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoSolapadoException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/** Spec carga-manual-turnos — carga y sobreturno por recepción (design D2, D3). */
@DisplayName("CargaManualDeTurnos")
class CargaManualDeTurnosTest {

    private static final Instant INICIO = Instant.parse("2026-12-01T12:00:00Z");
    private static final long USUARIO = 7L;

    private final ServicioDePacientes pacientes = mock(ServicioDePacientes.class);
    private final ServicioDeTurnos turnos = mock(ServicioDeTurnos.class);
    private final SugeridorDeAlternativas sugeridor = mock(SugeridorDeAlternativas.class);
    private final CargaManualDeTurnos carga = new CargaManualDeTurnos(pacientes, turnos, sugeridor);

    private final Paciente paciente = mock(Paciente.class);
    private final TurnoCreado creado = new TurnoCreado(mock(Turno.class), "token");

    private static PedidoDeCargaManual pedido() {
        return new PedidoDeCargaManual("30.123.456", "Ana", "Ejemplo", "+541155550000", "ana@example.com", 3L, 1L,
                2L, INICIO);
    }

    private void conPacienteYTurno() {
        when(paciente.getId()).thenReturn(5L);
        when(pacientes.registrarDesdeRecepcion(any())).thenReturn(paciente);
        when(turnos.crear(any())).thenReturn(creado);
    }

    private void conPacienteYTurnoRechazado(RuntimeException rechazo) {
        when(paciente.getId()).thenReturn(5L);
        when(pacientes.registrarDesdeRecepcion(any())).thenReturn(paciente);
        when(turnos.crear(any())).thenThrow(rechazo);
    }

    @Test
    @DisplayName("registra al paciente sin consentimiento antes de crear el turno de recepción, no sobreturno")
    void registraAlPacienteAntesYCreaElTurnoComun() {
        conPacienteYTurno();

        TurnoCreado resultado = carga.cargar(pedido(), USUARIO);

        assertThat(resultado).isSameAs(creado);
        InOrder orden = inOrder(pacientes, turnos);
        ArgumentCaptor<DatosPaciente> datos = ArgumentCaptor.forClass(DatosPaciente.class);
        orden.verify(pacientes).registrarDesdeRecepcion(datos.capture());
        ArgumentCaptor<SolicitudDeTurno> solicitud = ArgumentCaptor.forClass(SolicitudDeTurno.class);
        orden.verify(turnos).crear(solicitud.capture());

        assertThat(datos.getValue()).isEqualTo(
                new DatosPaciente("30.123.456", "Ana", "Ejemplo", "+541155550000", "ana@example.com", null));
        assertThat(solicitud.getValue()).isEqualTo(new SolicitudDeTurno(5L, 1L, 2L, 3L, INICIO,
                OrigenTurno.RECEPCION, false, Actor.usuario(USUARIO)));
    }

    @Test
    @DisplayName("el sobreturno usa la misma ruta pero con esSobreturno verdadero")
    void elSobreturnoMarcaLaSolicitud() {
        conPacienteYTurno();

        TurnoCreado resultado = carga.cargarSobreturno(pedido(), USUARIO);

        assertThat(resultado).isSameAs(creado);
        ArgumentCaptor<SolicitudDeTurno> solicitud = ArgumentCaptor.forClass(SolicitudDeTurno.class);
        InOrder orden = inOrder(pacientes, turnos);
        orden.verify(pacientes).registrarDesdeRecepcion(any());
        orden.verify(turnos).crear(solicitud.capture());
        assertThat(solicitud.getValue()).isEqualTo(new SolicitudDeTurno(5L, 1L, 2L, 3L, INICIO,
                OrigenTurno.RECEPCION, true, Actor.usuario(USUARIO)));
    }

    @Test
    @DisplayName("si el paciente es inválido no se crea ningún turno")
    void siElPacienteFallaNoCreaTurno() {
        when(pacientes.registrarDesdeRecepcion(any()))
                .thenThrow(new ValidacionDeDominioException("El email no es válido"));

        assertThatThrownBy(() -> carga.cargar(pedido(), USUARIO)).isInstanceOf(ValidacionDeDominioException.class);

        verify(turnos, never()).crear(any());
    }

    @Test
    @DisplayName("sobreturno rechazado por horario: propaga la excepción original sin consultar al motor")
    void sobreturnoFueraDeHorarioNoBuscaAlternativas() {
        AgendaNoDisponibleException original = new AgendaNoDisponibleException("fuera de horario");
        conPacienteYTurnoRechazado(original);

        assertThatThrownBy(() -> carga.cargarSobreturno(pedido(), USUARIO)).isSameAs(original);

        verifyNoInteractions(sugeridor);
    }

    @Test
    @DisplayName("solapamiento en la carga común: 409 con el mensaje original y las alternativas del sugeridor")
    void solapamientoDevuelveAlternativas() {
        TurnoSolapadoException original = new TurnoSolapadoException(RecursoOcupado.PROFESIONAL);
        conPacienteYTurnoRechazado(original);
        AlternativaDeTurno alternativa = new AlternativaDeTurno(INICIO.plusSeconds(3600), INICIO.plusSeconds(5400),
                1L, 2L);
        when(sugeridor.sugerir(3L, 1L, 2L, INICIO)).thenReturn(List.of(alternativa));

        assertThatThrownBy(() -> carga.cargar(pedido(), USUARIO))
                .isInstanceOf(ConflictoException.class)
                .isInstanceOfSatisfying(ConflictoConAlternativasException.class, e -> {
                    assertThat(e.getMessage()).isEqualTo(original.getMessage());
                    assertThat(e.alternativas()).containsExactly(alternativa);
                });
    }

    @Test
    @DisplayName("agenda no disponible en la carga común: también devuelve alternativas, aunque sean ninguna")
    void agendaNoDisponibleDevuelveAlternativas() {
        conPacienteYTurnoRechazado(new AgendaNoDisponibleException("El profesional no atiende en ese horario"));
        when(sugeridor.sugerir(3L, 1L, 2L, INICIO)).thenReturn(List.of());

        assertThatThrownBy(() -> carga.cargar(pedido(), USUARIO))
                .isInstanceOfSatisfying(ConflictoConAlternativasException.class, e -> {
                    assertThat(e.getMessage()).isEqualTo("El profesional no atiende en ese horario");
                    assertThat(e.alternativas()).isEmpty();
                });
    }

    @Test
    @DisplayName("un error de validación o de recurso no calcula alternativas")
    void otrosErroresSePropagan() {
        ValidacionDeDominioException original = new ValidacionDeDominioException("La prestación está inactiva");
        conPacienteYTurnoRechazado(original);

        assertThatThrownBy(() -> carga.cargar(pedido(), USUARIO)).isSameAs(original);

        verifyNoInteractions(sugeridor);
    }

    @Test
    @DisplayName("el pedido no imprime datos personales")
    void elPedidoNoImprimeDatosPersonales() {
        assertThat(pedido().toString()).doesNotContain("30.123.456", "Ana", "ana@example.com", "1155550000");
    }
}

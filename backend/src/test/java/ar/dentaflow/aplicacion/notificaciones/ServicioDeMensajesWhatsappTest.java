package ar.dentaflow.aplicacion.notificaciones;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.configuracion.ParametrosDelConsultorio;
import ar.dentaflow.aplicacion.turnos.ServicioDeTurnos;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.Turno;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** C-21 spec mensajes-whatsapp "Mensaje de WhatsApp de un turno a pedido" (design D1). */
class ServicioDeMensajesWhatsappTest {

    private static final long TURNO_ID = 7L;

    private final ServicioDeTurnos turnos = mock(ServicioDeTurnos.class);
    private final PacienteRepositorio pacientes = mock(PacienteRepositorio.class);
    private final PrestacionRepositorio prestaciones = mock(PrestacionRepositorio.class);
    private final ProfesionalRepositorio profesionales = mock(ProfesionalRepositorio.class);
    private final ParametrosDelConsultorio parametros = mock(ParametrosDelConsultorio.class);
    private final EnlacesDeTurno enlaces = mock(EnlacesDeTurno.class);
    private final Turno turno = mock(Turno.class);
    private final Paciente paciente = mock(Paciente.class);

    private ServicioDeMensajesWhatsapp servicio;

    @BeforeEach
    void preparar() {
        servicio = new ServicioDeMensajesWhatsapp(turnos, pacientes, prestaciones, profesionales, parametros, enlaces);
        Prestacion prestacion = mock(Prestacion.class);
        Profesional profesional = mock(Profesional.class);
        when(turno.getId()).thenReturn(TURNO_ID);
        when(turno.getEstado()).thenReturn(EstadoTurno.PENDIENTE);
        when(turno.getInicio()).thenReturn(Instant.parse("2026-12-05T13:00:00Z"));
        when(turno.getPacienteId()).thenReturn(1L);
        when(turno.getPrestacionId()).thenReturn(2L);
        when(turno.getProfesionalId()).thenReturn(3L);
        when(paciente.getNombre()).thenReturn("Ana");
        when(paciente.getTelefono()).thenReturn("2615550000");
        when(prestacion.getNombre()).thenReturn("Limpieza");
        when(profesional.getNombre()).thenReturn("Laura");
        when(profesional.getApellido()).thenReturn("Ejemplo");
        when(turnos.buscarPorId(TURNO_ID)).thenReturn(Optional.of(turno));
        when(pacientes.buscarPorId(1L)).thenReturn(Optional.of(paciente));
        when(prestaciones.buscarPorId(2L)).thenReturn(Optional.of(prestacion));
        when(profesionales.buscarPorId(3L)).thenReturn(Optional.of(profesional));
        when(parametros.plantillaWhatsapp()).thenReturn("Hola {nombre}, {prestacion} con {profesional} el {fecha} {hora}: {enlace}");
        when(enlaces.confirmar(turno)).thenReturn("http://front/turno/tok/confirmar");
    }

    @Test
    void armaElMensajeYElEnlaceDeUnTurnoPendienteConTelefonoValido() {
        MensajeWhatsapp mensaje = servicio.prepararPara(TURNO_ID);

        assertThat(mensaje.mensaje())
                .isEqualTo("Hola Ana, Limpieza con Laura Ejemplo el 05/12/2026 10:00: http://front/turno/tok/confirmar");
        assertThat(mensaje.enlace()).startsWith("https://wa.me/5492615550000?text=Hola%20Ana");
    }

    @Test
    void unTurnoConfirmadoTambienTieneMensaje() {
        when(turno.getEstado()).thenReturn(EstadoTurno.CONFIRMADO);

        assertThat(servicio.prepararPara(TURNO_ID).enlace()).isNotNull();
    }

    @Test
    void sinTelefonoUtilizableDevuelveElMensajeSinEnlace() {
        when(paciente.getTelefono()).thenReturn("00000000");

        MensajeWhatsapp mensaje = servicio.prepararPara(TURNO_ID);

        assertThat(mensaje.enlace()).isNull();
        assertThat(mensaje.mensaje()).startsWith("Hola Ana");
    }

    @Test
    void unTurnoInexistenteEsRecursoNoEncontrado() {
        when(turnos.buscarPorId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.prepararPara(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoTurno.class, names = {"CANCELADO", "ASISTIO", "AUSENTE", "AUSENTE_CON_AVISO"})
    void unTurnoNoVigenteEsUnConflicto(EstadoTurno estado) {
        when(turno.getEstado()).thenReturn(estado);

        assertThatThrownBy(() -> servicio.prepararPara(TURNO_ID))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("pendientes o confirmados");
    }
}

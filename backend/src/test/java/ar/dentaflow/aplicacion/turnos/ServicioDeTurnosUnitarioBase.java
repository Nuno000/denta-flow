package ar.dentaflow.aplicacion.turnos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.auditoria.ServicioDeAuditoria;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import ar.dentaflow.dominio.recursos.Prestacion;
import ar.dentaflow.dominio.recursos.PrestacionRepositorio;
import ar.dentaflow.dominio.recursos.Profesional;
import ar.dentaflow.dominio.recursos.ProfesionalRepositorio;
import ar.dentaflow.dominio.recursos.Sillon;
import ar.dentaflow.dominio.recursos.SillonRepositorio;
import ar.dentaflow.dominio.turnos.CerrojoDeRecursos;
import ar.dentaflow.dominio.turnos.EstadoTurno;
import ar.dentaflow.dominio.turnos.GeneradorDeTokens;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import ar.dentaflow.dominio.turnos.RestriccionDeAgenda;
import ar.dentaflow.dominio.turnos.Turno;
import ar.dentaflow.dominio.turnos.TurnoRepositorio;
import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;

/** Armado común de los tests unitarios (Mockito) de {@link ServicioDeTurnos}: reloj fijo y recursos de ejemplo. */
abstract class ServicioDeTurnosUnitarioBase {

    protected static final Instant AHORA = Instant.parse("2026-12-01T11:00:00Z");
    protected static final Instant INICIO = Instant.parse("2026-12-01T12:00:00Z");
    protected static final String TOKEN = "t".repeat(43);

    protected static final long PACIENTE_ID = 7L;
    protected static final long PROFESIONAL_ID = 3L;
    protected static final long SILLON_ID = 2L;
    protected static final long PRESTACION_ID = 5L;
    protected static final long TURNO_ID = 55L;
    protected static final long USUARIO_ID = 9L;

    protected PacienteRepositorio pacientes;
    protected PrestacionRepositorio prestaciones;
    protected ProfesionalRepositorio profesionales;
    protected SillonRepositorio sillones;
    protected TurnoRepositorio turnos;
    protected CerrojoDeRecursos cerrojo;
    protected GeneradorDeTokens generador;
    protected RestriccionDeAgenda restriccion;
    protected ServicioDeAuditoria auditoria;
    protected ServicioDeTurnos servicio;

    protected Prestacion prestacion;
    protected Profesional profesional;
    protected Sillon sillon;

    @BeforeEach
    void armarServicio() {
        pacientes = mock(PacienteRepositorio.class);
        prestaciones = mock(PrestacionRepositorio.class);
        profesionales = mock(ProfesionalRepositorio.class);
        sillones = mock(SillonRepositorio.class);
        turnos = mock(TurnoRepositorio.class);
        cerrojo = mock(CerrojoDeRecursos.class);
        generador = mock(GeneradorDeTokens.class);
        restriccion = mock(RestriccionDeAgenda.class);
        auditoria = mock(ServicioDeAuditoria.class);

        prestacion = AyudanteDeIds.conId(Prestacion.nueva("Limpieza", 30), PRESTACION_ID);
        profesional = AyudanteDeIds.conId(Profesional.nuevo("Laura", "Ejemplo", null, "laura@example.com"),
                PROFESIONAL_ID);
        profesional.habilitarPrestacion(prestacion);
        sillon = AyudanteDeIds.conId(Sillon.nuevo("Sillón 1"), SILLON_ID);
        Paciente paciente = AyudanteDeIds.conId(Paciente.nuevo("30123456", "Ana", "Ejemplo", "1155550000",
                "ana@example.com", Instant.parse("2026-11-01T10:00:00Z")), PACIENTE_ID);

        when(pacientes.buscarPorId(PACIENTE_ID)).thenReturn(Optional.of(paciente));
        when(prestaciones.buscarPorId(PRESTACION_ID)).thenReturn(Optional.of(prestacion));
        when(profesionales.buscarPorId(PROFESIONAL_ID)).thenReturn(Optional.of(profesional));
        when(sillones.buscarPorId(SILLON_ID)).thenReturn(Optional.of(sillon));
        when(generador.nuevoToken()).thenReturn(TOKEN);
        when(turnos.guardar(any())).thenAnswer(invocacion -> {
            Turno turno = invocacion.getArgument(0);
            return turno.getId() == null ? AyudanteDeIds.conId(turno, TURNO_ID) : turno;
        });

        @SuppressWarnings("unchecked")
        ObjectProvider<RestriccionDeAgenda> restricciones = mock(ObjectProvider.class);
        when(restricciones.orderedStream()).thenAnswer(invocacion -> Stream.of(restriccion));

        servicio = new ServicioDeTurnos(turnos, pacientes, prestaciones, profesionales, sillones, cerrojo, generador,
                restricciones, auditoria, Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    /** Un turno ya guardado (con id) de recepción, en el estado pedido. */
    protected Turno turnoExistente(EstadoTurno estado) {
        Turno turno = AyudanteDeIds.conId(Turno.nuevo(PACIENTE_ID, PROFESIONAL_ID, SILLON_ID, PRESTACION_ID, INICIO,
                30, OrigenTurno.RECEPCION, false, USUARIO_ID, AHORA, TOKEN), TURNO_ID);
        if (estado != EstadoTurno.PENDIENTE) {
            turno.cambiarEstado(estado);
        }
        return turno;
    }
}

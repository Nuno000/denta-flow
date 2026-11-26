package ar.dentaflow.aplicacion.agenda;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ar.dentaflow.aplicacion.disponibilidad.DisponibilidadDeRecursos;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.disponibilidad.AgendaNoDisponibleException;
import ar.dentaflow.dominio.turnos.TurnoPropuesto;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RestriccionDeHorarioYBloqueosTest {

    private static final Instant INICIO = Instant.parse("2026-12-01T13:00:00Z");
    private static final Instant FIN = Instant.parse("2026-12-01T13:30:00Z");

    private final DisponibilidadDeRecursos disponibilidad = mock(DisponibilidadDeRecursos.class);
    private final RestriccionDeHorarioYBloqueos restriccion = new RestriccionDeHorarioYBloqueos(disponibilidad);

    @Test
    void profesionalNoDisponibleLanzaAgendaNoDisponibleYNoConsultaSillon() {
        when(disponibilidad.profesionalDisponible(1L, INICIO, FIN)).thenReturn(false);

        assertThatThrownBy(() -> restriccion.verificar(new TurnoPropuesto(1L, 2L, 3L, INICIO, FIN, false)))
                .isInstanceOf(AgendaNoDisponibleException.class)
                .isInstanceOf(ConflictoException.class);

        verify(disponibilidad).profesionalDisponible(1L, INICIO, FIN);
        verifyNoMoreInteractions(disponibilidad);
    }

    @Test
    void sillonNoDisponibleLanzaAgendaNoDisponible() {
        when(disponibilidad.profesionalDisponible(1L, INICIO, FIN)).thenReturn(true);
        when(disponibilidad.sillonDisponible(2L, INICIO, FIN)).thenReturn(false);

        assertThatThrownBy(() -> restriccion.verificar(new TurnoPropuesto(1L, 2L, 3L, INICIO, FIN, false)))
                .isInstanceOf(AgendaNoDisponibleException.class);
    }

    @Test
    void ambosDisponiblesNoLanza() {
        when(disponibilidad.profesionalDisponible(1L, INICIO, FIN)).thenReturn(true);
        when(disponibilidad.sillonDisponible(2L, INICIO, FIN)).thenReturn(true);

        assertThatCode(() -> restriccion.verificar(new TurnoPropuesto(1L, 2L, 3L, INICIO, FIN, false)))
                .doesNotThrowAnyException();
    }

    @Test
    void aplicaIgualConSobreturnoTrue() {
        when(disponibilidad.profesionalDisponible(1L, INICIO, FIN)).thenReturn(false);

        assertThatThrownBy(() -> restriccion.verificar(new TurnoPropuesto(1L, 2L, 3L, INICIO, FIN, true)))
                .isInstanceOf(AgendaNoDisponibleException.class);
    }
}

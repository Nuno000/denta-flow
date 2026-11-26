package ar.dentaflow.aplicacion.auditoria;

import ar.dentaflow.dominio.auditoria.AccionDeTurno;
import ar.dentaflow.dominio.auditoria.TipoDeActor;
import java.time.Instant;
import java.util.Map;

/** Entrada del registro de cambios de un turno enriquecida con el email del usuario interno (US-025). */
public record EntradaDeHistorial(
        Long id,
        Instant ocurridoEn,
        TipoDeActor actorTipo,
        String actorEmail,
        AccionDeTurno accion,
        Map<String, String> valorAnterior,
        Map<String, String> valorNuevo) {
}

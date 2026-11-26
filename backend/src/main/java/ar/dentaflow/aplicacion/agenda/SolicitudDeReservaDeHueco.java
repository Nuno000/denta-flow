package ar.dentaflow.aplicacion.agenda;

import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import java.time.Instant;

/** Datos necesarios para reservar un hueco sin elegir sillón (design D6). */
public record SolicitudDeReservaDeHueco(
        long pacienteId,
        long prestacionId,
        Long profesionalId,
        Instant inicio,
        OrigenTurno origen,
        Actor actor) {
}

package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.dominio.auditoria.Actor;
import ar.dentaflow.dominio.turnos.OrigenTurno;
import java.time.Instant;

/**
 * Pedido de alta de un turno. {@code actor} es {@code Actor.paciente()} para {@code ONLINE} y
 * {@code Actor.usuario(id)} para {@code RECEPCION}; el servicio lo verifica. El rol del usuario (recepción o
 * administrador, no profesional, RN-SO-01 para el sobreturno) lo verifica quien llama.
 */
public record SolicitudDeTurno(long pacienteId, long profesionalId, long sillonId, long prestacionId, Instant inicio,
        OrigenTurno origen, boolean esSobreturno, Actor actor) {
}

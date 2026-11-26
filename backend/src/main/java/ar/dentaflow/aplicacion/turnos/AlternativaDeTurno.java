package ar.dentaflow.aplicacion.turnos;

import java.time.Instant;

/** Horario libre sugerido ante un conflicto de la carga manual (C-12, design D3). */
public record AlternativaDeTurno(Instant inicio, Instant fin, long profesionalId, long sillonId) {
}

package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.dominio.notificaciones.SituacionDeEnlace;
import java.time.Instant;

/**
 * Detalle público de un turno resuelto a partir de su token para el paciente.
 * No expone datos personales ni identificadores internos.
 */
public record TurnoPublicoDetalle(
        SituacionDeEnlace situacion,
        Instant inicio,
        Instant fin,
        String prestacion,
        String profesional,
        String estado,
        boolean puedeModificar) {
}

package ar.dentaflow.dominio.disponibilidad;

/** Motivo de un bloqueo de agenda; {@code OTRO} se acompaña de un detalle libre. */
public enum MotivoBloqueo {
    VACACIONES,
    FERIADO,
    AUSENCIA,
    OTRO
}

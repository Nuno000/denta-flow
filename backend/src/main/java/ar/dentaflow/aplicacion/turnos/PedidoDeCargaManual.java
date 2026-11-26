package ar.dentaflow.aplicacion.turnos;

import java.time.Instant;

/**
 * Pedido de carga manual de un turno por el personal (C-12): los datos de contacto del paciente, los recursos y el
 * inicio ya convertido a instante. Es de la capa de aplicación (no el DTO de presentación) para no acoplarla a
 * Jackson ni a Bean Validation.
 */
public record PedidoDeCargaManual(String dni, String nombre, String apellido, String telefono, String email,
        long prestacionId, long profesionalId, long sillonId, Instant inicio) {

    /** Evita imprimir datos personales en logs o mensajes de error. */
    @Override
    public String toString() {
        return "PedidoDeCargaManual[...]";
    }
}

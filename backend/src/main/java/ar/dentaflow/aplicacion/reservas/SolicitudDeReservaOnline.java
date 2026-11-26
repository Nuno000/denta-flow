package ar.dentaflow.aplicacion.reservas;

import java.time.Instant;

/**
 * Datos necesarios para procesar una reserva online (C-16 design D2).
 */
public record SolicitudDeReservaOnline(
        long prestacionId,
        Long profesionalId,
        Instant inicio,
        String dni,
        String nombre,
        String apellido,
        String telefono,
        String email,
        Boolean aceptaAvisoPrivacidad,
        String origenIp) {
}

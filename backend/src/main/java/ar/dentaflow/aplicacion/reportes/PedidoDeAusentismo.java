package ar.dentaflow.aplicacion.reportes;

import java.time.LocalDate;

/** Parámetros de entrada para consultar indicadores de ausentismo (C-23). */
public record PedidoDeAusentismo(
        LocalDate desde,
        LocalDate hasta,
        Long profesionalId,
        Integer minimoAusencias) {
}

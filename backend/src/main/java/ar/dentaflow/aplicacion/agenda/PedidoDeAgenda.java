package ar.dentaflow.aplicacion.agenda;

import java.time.LocalDate;

/**
 * Lo que pide la pantalla de agenda (C-11, design D1): días del consultorio {@code desde} y {@code hasta}, ambos
 * inclusivos, y filtros opcionales. {@code estado} llega como código ({@code pendiente}, {@code cancelado}, ...).
 */
public record PedidoDeAgenda(LocalDate desde, LocalDate hasta, Long profesionalId, Long sillonId, String estado) {
}

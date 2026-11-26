package ar.dentaflow.dominio.disponibilidad;

/** Par ordenado (profesional, sillón) candidato para ocupar un hueco. */
public record CandidatoDeReserva(Long profesionalId, Long sillonId) {
}

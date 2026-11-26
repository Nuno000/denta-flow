package ar.dentaflow.dominio.turnos;

/** Resultado de {@link Turno#cambiarEstado}: estado anterior y nuevo, o sin cambio si ya estaba en ese estado. */
public record CambioDeEstado(EstadoTurno anterior, EstadoTurno nuevo) {

    public static CambioDeEstado sinCambio(EstadoTurno estado) {
        return new CambioDeEstado(estado, estado);
    }

    public boolean hayCambio() {
        return anterior != nuevo;
    }
}

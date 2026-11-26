package ar.dentaflow.dominio.turnos;

import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;

/** El turno no puede pasar de su estado actual al pedido (409). */
public class TransicionDeEstadoInvalidaException extends OperacionNoPermitidaException {

    public TransicionDeEstadoInvalidaException(EstadoTurno desde, EstadoTurno hacia) {
        super("Un turno " + desde.codigo() + " no puede pasar a " + hacia.codigo());
    }

    public TransicionDeEstadoInvalidaException(EstadoTurno desde, String operacion) {
        super("No se puede " + operacion + " un turno " + desde.codigo());
    }
}

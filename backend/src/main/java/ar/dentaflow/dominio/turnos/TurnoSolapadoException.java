package ar.dentaflow.dominio.turnos;

import ar.dentaflow.dominio.comun.ConflictoException;

/** El horario pedido ya está ocupado por otro turno del mismo profesional o del mismo sillón (409). */
public class TurnoSolapadoException extends ConflictoException {

    private final RecursoOcupado recurso;

    public TurnoSolapadoException(RecursoOcupado recurso) {
        super("El horario ya está ocupado: el " + recurso.descripcion() + " tiene otro turno en ese horario");
        this.recurso = recurso;
    }

    public RecursoOcupado recurso() {
        return recurso;
    }
}

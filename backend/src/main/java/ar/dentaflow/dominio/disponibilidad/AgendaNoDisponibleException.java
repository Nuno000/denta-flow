package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ConflictoException;

/** El profesional o el sillón no atiende o tiene un bloqueo en el horario pedido (RN-TU-04). */
public class AgendaNoDisponibleException extends ConflictoException {

    public AgendaNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}

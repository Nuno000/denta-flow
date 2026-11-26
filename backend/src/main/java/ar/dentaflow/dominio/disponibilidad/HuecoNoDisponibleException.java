package ar.dentaflow.dominio.disponibilidad;

import ar.dentaflow.dominio.comun.ConflictoException;

/** Todos los candidatos para el hueco elegido dejaron de estar disponibles al confirmar (409). */
public class HuecoNoDisponibleException extends ConflictoException {

    public HuecoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}

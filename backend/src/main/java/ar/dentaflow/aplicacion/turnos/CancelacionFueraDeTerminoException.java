package ar.dentaflow.aplicacion.turnos;

import ar.dentaflow.dominio.comun.ConflictoException;

/**
 * Se intentó cancelar o reprogramar un turno fuera del plazo de anticipación mínima permitida (409).
 */
public class CancelacionFueraDeTerminoException extends ConflictoException {

    public CancelacionFueraDeTerminoException(String mensaje) {
        super(mensaje);
    }

    public static CancelacionFueraDeTerminoException conContacto(String contacto) {
        String base = "El plazo de cancelación o reprogramación online ha vencido. Por favor comunicate directamente con el consultorio.";
        if (contacto != null && !contacto.isBlank()) {
            return new CancelacionFueraDeTerminoException(base + " (" + contacto + ")");
        }
        return new CancelacionFueraDeTerminoException(base);
    }
}

package ar.dentaflow.dominio.comun;

/**
 * La operación choca con el estado actual (p. ej. un email ya usado). La capa de presentación lo
 * traduce a un error 409.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}

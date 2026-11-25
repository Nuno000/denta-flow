package ar.dentaflow.dominio.comun;

/**
 * La operación es válida en general pero no está permitida en este caso (p. ej. que el administrador
 * se desactive a sí mismo). La capa de presentación lo traduce a un error 409.
 */
public class OperacionNoPermitidaException extends RuntimeException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}

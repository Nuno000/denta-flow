package ar.dentaflow.aplicacion.usuarios;

/**
 * Ingreso rechazado. El mensaje es siempre el mismo para email inexistente, contraseña incorrecta o
 * usuario desactivado, para no revelar cuál ocurrió (spec "Rechazo genérico de credenciales inválidas").
 */
public class CredencialesInvalidasException extends RuntimeException {

    public static final String MENSAJE = "Email o contraseña incorrectos";

    public CredencialesInvalidasException() {
        super(MENSAJE);
    }
}

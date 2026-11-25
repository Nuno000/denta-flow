package ar.dentaflow.dominio.comun;

/** El recurso pedido no existe. La capa de presentación lo traduce a un error 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

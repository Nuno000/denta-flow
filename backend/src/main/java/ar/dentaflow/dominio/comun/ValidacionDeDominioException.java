package ar.dentaflow.dominio.comun;

/**
 * Se lanza cuando un dato rompe una invariante del dominio. El mensaje, en español, nombra el dato
 * afectado; la capa de presentación lo traducirá a un error 400.
 */
public class ValidacionDeDominioException extends RuntimeException {

    public ValidacionDeDominioException(String mensaje) {
        super(mensaje);
    }
}

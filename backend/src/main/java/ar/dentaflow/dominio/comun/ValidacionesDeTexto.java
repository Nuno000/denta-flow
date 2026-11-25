package ar.dentaflow.dominio.comun;

/** Validaciones de texto compartidas por los recursos del dominio. */
public final class ValidacionesDeTexto {

    private ValidacionesDeTexto() {
    }

    /** Devuelve el texto recortado; falla si es nulo, está en blanco o supera {@code maximo}. */
    public static String obligatorio(String texto, String dato, int maximo) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionDeDominioException("El " + dato + " es obligatorio");
        }
        return acotado(texto.strip(), dato, maximo);
    }

    /** Devuelve el texto recortado, o {@code null} si es nulo o está en blanco; falla si supera {@code maximo}. */
    public static String opcional(String texto, String dato, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return acotado(texto.strip(), dato, maximo);
    }

    private static String acotado(String texto, String dato, int maximo) {
        if (texto.length() > maximo) {
            throw new ValidacionDeDominioException("El " + dato + " no puede superar los " + maximo + " caracteres");
        }
        return texto;
    }
}

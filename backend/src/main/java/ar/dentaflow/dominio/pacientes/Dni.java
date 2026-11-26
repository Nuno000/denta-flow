package ar.dentaflow.dominio.pacientes;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.regex.Pattern;

/** Normalización del DNI argentino (design D3, Q13): se guarda sin puntos, espacios ni guiones, de 7 u 8 dígitos. */
public final class Dni {

    private static final Pattern SEPARADORES = Pattern.compile("[.\\s-]");
    private static final Pattern SIETE_U_OCHO_DIGITOS = Pattern.compile("\\d{7,8}");

    private Dni() {
    }

    /** Devuelve el DNI solo con dígitos; falla si es nulo, está en blanco o no tiene 7 u 8 dígitos. */
    public static String normalizar(String escrito) {
        if (escrito == null || escrito.isBlank()) {
            throw new ValidacionDeDominioException("El DNI es obligatorio");
        }
        String normalizado = SEPARADORES.matcher(escrito.strip()).replaceAll("");
        if (!SIETE_U_OCHO_DIGITOS.matcher(normalizado).matches()) {
            throw new ValidacionDeDominioException("El DNI debe tener 7 u 8 dígitos");
        }
        return normalizado;
    }
}

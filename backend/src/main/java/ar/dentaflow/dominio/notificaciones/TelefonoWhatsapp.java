package ar.dentaflow.dominio.notificaciones;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normaliza el teléfono del paciente al formato que usa {@code wa.me} para un celular argentino (C-21, design D2):
 * {@code 549} + código de área sin {@code 0} + número sin {@code 15}, 13 dígitos en total. Acepta el formato
 * nacional (con o sin {@code 0} y {@code 15}) y el internacional {@code +54}/{@code 0054} (con o sin {@code 9}).
 * No valida contra la numeración oficial: lo que no encaja se descarta y el panel muestra un aviso.
 */
public final class TelefonoWhatsapp {

    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\-().]");
    private static final Pattern SOLO_DIGITOS = Pattern.compile("\\d+");
    private static final Pattern UN_SOLO_DIGITO_REPETIDO = Pattern.compile("(\\d)\\1*");
    private static final String PREFIJO_CELULAR_ARGENTINO = "549";
    private static final int DIGITOS_NACIONALES = 10;

    private TelefonoWhatsapp() {
    }

    /** Dígitos para {@code wa.me}, o vacío si el teléfono no es un número argentino utilizable. */
    public static Optional<String> normalizar(String telefono) {
        if (telefono == null) {
            return Optional.empty();
        }
        String limpio = SEPARADORES.matcher(telefono).replaceAll("");
        boolean internacional = limpio.startsWith("+") || limpio.startsWith("00");
        String digitos = limpio.startsWith("+") ? limpio.substring(1) : limpio.startsWith("00") ? limpio.substring(2) : limpio;
        if (!SOLO_DIGITOS.matcher(digitos).matches() || UN_SOLO_DIGITO_REPETIDO.matcher(digitos).matches()) {
            return Optional.empty();
        }
        Optional<String> nacional = internacional ? desdeInternacional(digitos) : desdeNacional(digitos);
        return nacional.map(numero -> PREFIJO_CELULAR_ARGENTINO + numero);
    }

    private static Optional<String> desdeInternacional(String digitos) {
        if (!digitos.startsWith("54")) {
            return Optional.empty();
        }
        String resto = digitos.substring(2);
        if (resto.startsWith("9")) {
            String numero = resto.substring(1);
            return numero.length() == DIGITOS_NACIONALES ? Optional.of(numero) : Optional.empty();
        }
        return desdeNacional(resto);
    }

    private static Optional<String> desdeNacional(String digitos) {
        String sinCero = digitos.startsWith("0") ? digitos.substring(1) : digitos;
        if (sinCero.length() == DIGITOS_NACIONALES) {
            return Optional.of(sinCero);
        }
        if (sinCero.length() == DIGITOS_NACIONALES + 2) {
            return quitarQuince(sinCero);
        }
        return Optional.empty();
    }

    /** Quita el {@code 15} de celular que sigue al código de área (2 dígitos si es 11; si no, 3 o 4). */
    private static Optional<String> quitarQuince(String numero) {
        int[] largosDeArea = numero.startsWith("11") ? new int[] {2} : new int[] {3, 4};
        for (int largo : largosDeArea) {
            if (numero.startsWith("15", largo)) {
                return Optional.of(numero.substring(0, largo) + numero.substring(largo + 2));
            }
        }
        return Optional.empty();
    }
}

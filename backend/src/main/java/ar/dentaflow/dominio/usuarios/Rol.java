package ar.dentaflow.dominio.usuarios;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Roles internos del consultorio (RN-AU-02). Un usuario tiene uno solo mientras PA-09 siga abierta.
 * El código coincide con la clave de la tabla {@code rol} y con la KB; las guardas de autorización se
 * basan en este enum, no en filas editables (design D4).
 */
public enum Rol {

    ADMINISTRADOR("administrador"),
    RECEPCION("recepcion"),
    PROFESIONAL("profesional");

    private final String codigo;

    Rol(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    /** Busca el rol por su código; falla con los códigos válidos si no existe. */
    public static Rol desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(rol -> rol.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException(
                        "El rol '" + codigo + "' no existe. Roles válidos: " + codigosValidos()));
    }

    private static String codigosValidos() {
        return Arrays.stream(values()).map(Rol::codigo).collect(Collectors.joining(", "));
    }
}

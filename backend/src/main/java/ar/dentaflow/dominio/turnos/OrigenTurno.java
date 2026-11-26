package ar.dentaflow.dominio.turnos;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/** Canal por el que se creó el turno ({@code ck_turno_origen}). */
public enum OrigenTurno {
    ONLINE("online"),
    RECEPCION("recepcion");

    private final String codigo;

    OrigenTurno(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static OrigenTurno desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(origen -> origen.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Origen de turno desconocido: " + codigo));
    }
}

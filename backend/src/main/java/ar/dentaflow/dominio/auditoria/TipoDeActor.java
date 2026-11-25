package ar.dentaflow.dominio.auditoria;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;

/** Quién puede cambiar un turno (spec "Actor identificado"). El código es el que se guarda en la base. */
public enum TipoDeActor {
    /** Personal autenticado del consultorio: siempre con id de usuario. */
    USUARIO("usuario"),
    /** El paciente, desde el enlace de su turno. */
    PACIENTE("paciente"),
    /** Tareas automáticas (recordatorios, vencimientos). */
    SISTEMA("sistema");

    private final String codigo;

    TipoDeActor(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static TipoDeActor desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(tipo -> tipo.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Tipo de actor desconocido: " + codigo));
    }
}

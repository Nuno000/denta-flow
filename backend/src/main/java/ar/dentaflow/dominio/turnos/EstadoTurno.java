package ar.dentaflow.dominio.turnos;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de un turno con su código de base ({@code ck_turno_estado}) y la tabla de transiciones como única
 * fuente (design D4, Q6). Repetir el estado actual es idempotente. {@code cancelado} es final; la asistencia
 * se puede corregir entre {@code asistio}, {@code ausente} y {@code ausente_con_aviso}.
 */
public enum EstadoTurno {
    PENDIENTE("pendiente"),
    CONFIRMADO("confirmado"),
    CANCELADO("cancelado"),
    ASISTIO("asistio"),
    AUSENTE("ausente"),
    AUSENTE_CON_AVISO("ausente_con_aviso");

    private static final Set<EstadoTurno> ASISTENCIA = EnumSet.of(ASISTIO, AUSENTE, AUSENTE_CON_AVISO);

    private final String codigo;

    EstadoTurno(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static EstadoTurno desdeCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(estado -> estado.codigo.equals(codigo))
                .findFirst()
                .orElseThrow(() -> new ValidacionDeDominioException("Estado de turno desconocido: " + codigo));
    }

    /** {@code true} si pasar de este estado a {@code destino} es válido; el mismo estado cuenta como válido (no-op). */
    public boolean puedePasarA(EstadoTurno destino) {
        if (destino == this) {
            return true;
        }
        return switch (this) {
            case PENDIENTE, CONFIRMADO -> destino != PENDIENTE;
            case CANCELADO -> false;
            case ASISTIO, AUSENTE, AUSENTE_CON_AVISO -> ASISTENCIA.contains(destino);
        };
    }

    /** Solo el turno cancelado libera al profesional y al sillón. */
    public boolean ocupaRecursos() {
        return this != CANCELADO;
    }
}

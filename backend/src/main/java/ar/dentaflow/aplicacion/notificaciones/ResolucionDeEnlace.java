package ar.dentaflow.aplicacion.notificaciones;

import ar.dentaflow.dominio.notificaciones.SituacionDeEnlace;
import java.util.Optional;

/** Resultado de resolver un token: la situación y, salvo {@code INEXISTENTE}, el resumen del turno. */
public record ResolucionDeEnlace(SituacionDeEnlace situacion, Optional<ResumenTurnoPaciente> resumen) {

    static ResolucionDeEnlace inexistente() {
        return new ResolucionDeEnlace(SituacionDeEnlace.INEXISTENTE, Optional.empty());
    }
}

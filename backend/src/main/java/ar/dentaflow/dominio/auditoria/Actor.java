package ar.dentaflow.dominio.auditoria;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;

/**
 * Quién hizo un cambio de turno (RN-AU-01). El id de usuario existe solo para el personal; las fábricas
 * hacen imposible un actor incoherente (design D2). Quien llama a la auditoría arma el actor (design D6).
 */
public record Actor(TipoDeActor tipo, Long usuarioId) {

    public Actor {
        if (tipo == null) {
            throw new ValidacionDeDominioException("El tipo de actor es obligatorio");
        }
        if (tipo == TipoDeActor.USUARIO && (usuarioId == null || usuarioId <= 0)) {
            throw new ValidacionDeDominioException("El actor usuario necesita un id de usuario válido");
        }
        if (tipo != TipoDeActor.USUARIO && usuarioId != null) {
            throw new ValidacionDeDominioException("Solo el actor usuario lleva id de usuario");
        }
    }

    public static Actor usuario(Long usuarioId) {
        return new Actor(TipoDeActor.USUARIO, usuarioId);
    }

    public static Actor paciente() {
        return new Actor(TipoDeActor.PACIENTE, null);
    }

    public static Actor sistema() {
        return new Actor(TipoDeActor.SISTEMA, null);
    }
}

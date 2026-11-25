package ar.dentaflow.presentacion.auth;

import ar.dentaflow.presentacion.usuarios.UsuarioRespuesta;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** Resultado del ingreso: el token para el header {@code Authorization: Bearer} y cuándo vence (UTC). */
public record LoginRespuesta(
        @Schema(description = "JWT para enviar en Authorization: Bearer") String token,
        @Schema(description = "Vencimiento del token, en UTC") Instant venceEn,
        UsuarioRespuesta usuario) {
}

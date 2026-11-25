package ar.dentaflow.presentacion.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Cambio de rol de un usuario, con el profesional a vincular si el rol nuevo es profesional. */
public record CambiarRolSolicitud(
        @NotBlank(message = "El rol es obligatorio")
        @Schema(allowableValues = {"administrador", "recepcion", "profesional"})
        String rol,
        @Schema(description = "Obligatorio solo con el rol profesional")
        Long profesionalId) {
}

package ar.dentaflow.presentacion.usuarios;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta de un usuario. El email lo valida y normaliza el dominio; el rol, por su código. */
public record CrearUsuarioSolicitud(
        @NotBlank(message = "El email es obligatorio")
        @Size(max = 254, message = "El email no puede superar los 254 caracteres")
        String email,
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena,
        @NotBlank(message = "El rol es obligatorio")
        @Schema(allowableValues = {"administrador", "recepcion", "profesional"})
        String rol,
        @Schema(description = "Obligatorio solo con el rol profesional")
        Long profesionalId) {

    /** Sin la contraseña, por si el objeto termina en un log. */
    @Override
    public String toString() {
        return "CrearUsuarioSolicitud[email=" + email + ", rol=" + rol + ", profesionalId=" + profesionalId + "]";
    }
}

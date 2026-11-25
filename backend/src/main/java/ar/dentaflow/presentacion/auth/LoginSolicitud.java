package ar.dentaflow.presentacion.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Datos de ingreso del personal. El email admite espacios en los extremos y cualquier combinación de
 * mayúsculas (spec "Ingreso con email y contraseña"): el dominio lo normaliza antes de buscarlo.
 */
public record LoginSolicitud(
        @NotBlank(message = "El email es obligatorio")
        @Pattern(regexp = "\\s*[^@\\s]+@[^@\\s]+\\s*", message = "El email no tiene un formato válido")
        String email,
        @NotBlank(message = "La contraseña es obligatoria") String contrasena) {

    /** Sin la contraseña, por si el objeto termina en un log. */
    @Override
    public String toString() {
        return "LoginSolicitud[email=" + email + "]";
    }
}

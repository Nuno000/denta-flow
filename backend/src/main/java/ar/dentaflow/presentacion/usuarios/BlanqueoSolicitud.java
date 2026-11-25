package ar.dentaflow.presentacion.usuarios;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Contraseña nueva que el administrador define para otro usuario. */
public record BlanqueoSolicitud(
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena) {

    /** Sin la contraseña, por si el objeto termina en un log. */
    @Override
    public String toString() {
        return "BlanqueoSolicitud[***]";
    }
}

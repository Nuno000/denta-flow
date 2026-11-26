package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.dominio.recursos.Profesional;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta o edición de los datos de un profesional (design D3). El formato del email lo valida el dominio;
 * una matrícula vacía queda sin matrícula.
 */
public record ProfesionalSolicitud(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = Profesional.NOMBRE_MAXIMO, message = "El nombre no puede superar los 100 caracteres")
        String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = Profesional.APELLIDO_MAXIMO, message = "El apellido no puede superar los 100 caracteres")
        String apellido,
        @Schema(description = "Opcional; vacía o nula deja al profesional sin matrícula")
        @Size(max = Profesional.MATRICULA_MAXIMO, message = "La matrícula no puede superar los 30 caracteres")
        String matricula,
        @NotBlank(message = "El email es obligatorio")
        @Size(max = Profesional.EMAIL_MAXIMO, message = "El email no puede superar los 254 caracteres")
        String email) {
}

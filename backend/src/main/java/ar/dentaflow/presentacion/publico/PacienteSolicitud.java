package ar.dentaflow.presentacion.publico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PacienteSolicitud(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "^\\d{7,8}$", message = "El DNI debe tener 7 u 8 dígitos sin puntos")
        @Schema(description = "DNI de 7 u 8 dígitos sin puntos ni espacios", example = "30111222")
        String dni,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede tener más de 100 caracteres")
        @Schema(description = "Nombre del paciente", example = "Ana")
        String nombre,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede tener más de 100 caracteres")
        @Schema(description = "Apellido del paciente", example = "García")
        String apellido,

        @NotBlank(message = "El teléfono es obligatorio")
        @Schema(description = "Teléfono de contacto con código de área", example = "0261 555-1234")
        String telefono,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 254, message = "El email no puede tener más de 254 caracteres")
        @Schema(description = "Email donde se envían los enlaces de confirmación y cancelación", example = "ana@example.com")
        String email) {
}

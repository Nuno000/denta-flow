package ar.dentaflow.presentacion.configuracion;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Valor nuevo de un parámetro; viaja siempre como texto y se valida según el tipo de la clave. */
public record ActualizarParametroSolicitud(
        @NotNull(message = "El valor es obligatorio")
        @Schema(description = "Valor nuevo, como texto", example = "48")
        String valor) {
}

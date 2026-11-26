package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.dominio.recursos.Sillon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Alta o edición de un sillón (design D3). */
public record SillonSolicitud(
        @NotBlank(message = "El nombre del sillón es obligatorio")
        @Size(max = Sillon.NOMBRE_MAXIMO, message = "El nombre del sillón no puede superar los 50 caracteres")
        String nombre) {
}

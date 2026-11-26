package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.dominio.recursos.Prestacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Alta o edición de una prestación: la duración en minutos define la del turno (RN-TU-03, design D3). */
public record PrestacionSolicitud(
        @NotBlank(message = "El nombre de la prestación es obligatorio")
        @Size(max = Prestacion.NOMBRE_MAXIMO, message = "El nombre de la prestación no puede superar los 100 caracteres")
        String nombre,
        @NotNull(message = "La duración es obligatoria")
        @Positive(message = "La duración debe ser mayor que cero")
        Integer duracionMinutos) {
}

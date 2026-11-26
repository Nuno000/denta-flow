package ar.dentaflow.presentacion.disponibilidad;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** Alta o edición de un rango de atención, en hora del consultorio. */
public record HorarioSolicitud(
        @Schema(example = "LUNES") @NotNull(message = "El día de la semana es obligatorio") DiaSemana dia,
        @Schema(type = "string", example = "09:00") @NotNull(message = "La hora de inicio es obligatoria") LocalTime inicio,
        @Schema(type = "string", example = "13:00") @NotNull(message = "La hora de fin es obligatoria") LocalTime fin) {
}

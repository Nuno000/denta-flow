package ar.dentaflow.presentacion.publico;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ReprogramarSolicitud(
        @NotNull(message = "El nuevo horario de inicio es obligatorio")
        @Schema(description = "Nuevo horario de inicio del turno con offset -03:00", example = "2026-12-05T10:00:00-03:00")
        OffsetDateTime inicio) {
}

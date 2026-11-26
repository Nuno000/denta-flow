package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Edición de un bloqueo: cambia franja, motivo y detalle. Ámbito y recurso son opcionales y solo se
 * aceptan si coinciden con los guardados (spec "Intento de cambiar el ámbito").
 */
public record BloqueoEdicion(
        @Schema(example = "2026-12-24T08:00:00-03:00") @NotNull(message = "El inicio es obligatorio") OffsetDateTime inicio,
        @Schema(example = "2026-12-24T13:00:00-03:00") @NotNull(message = "El fin es obligatorio") OffsetDateTime fin,
        @Schema(example = "VACACIONES") @NotNull(message = "El motivo es obligatorio") MotivoBloqueo motivo,
        @Schema(description = "Texto libre opcional, hasta 200 caracteres") String detalle,
        @Schema(description = "No se edita: si viene debe coincidir con el guardado") AmbitoBloqueo ambito,
        @Schema(description = "No se edita: si viene debe coincidir con el guardado") Long profesionalId,
        @Schema(description = "No se edita: si viene debe coincidir con el guardado") Long sillonId) {
}

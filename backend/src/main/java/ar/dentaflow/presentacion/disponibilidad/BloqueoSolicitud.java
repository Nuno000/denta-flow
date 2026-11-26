package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Alta de un bloqueo. Los instantes llevan desplazamiento obligatorio (design D8, Q6): un texto sin
 * desplazamiento no se puede leer y responde 400. La coherencia entre ámbito y recurso la valida el
 * servicio.
 */
public record BloqueoSolicitud(
        @Schema(example = "PROFESIONAL") @NotNull(message = "El ámbito es obligatorio") AmbitoBloqueo ambito,
        @Schema(description = "Solo para el ámbito PROFESIONAL") Long profesionalId,
        @Schema(description = "Solo para el ámbito SILLON") Long sillonId,
        @Schema(example = "2026-12-24T08:00:00-03:00") @NotNull(message = "El inicio es obligatorio") OffsetDateTime inicio,
        @Schema(example = "2026-12-24T13:00:00-03:00") @NotNull(message = "El fin es obligatorio") OffsetDateTime fin,
        @Schema(example = "VACACIONES") @NotNull(message = "El motivo es obligatorio") MotivoBloqueo motivo,
        @Schema(description = "Texto libre opcional, hasta 200 caracteres") String detalle) {
}

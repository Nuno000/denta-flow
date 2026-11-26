package ar.dentaflow.presentacion.turnos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Pedido para registrar la asistencia de un turno (C-13, design D3). */
public record SolicitudDeAsistencia(
        @NotBlank(message = "El estado es obligatorio")
        @Schema(description = "Estado de asistencia del turno",
                allowableValues = {"asistio", "ausente", "ausente_con_aviso"})
        String estado) {
}

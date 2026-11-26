package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record ReservaSolicitud(
        @NotNull(message = "La prestación es obligatoria")
        @Schema(description = "ID de la prestación activa que se desea reservar", example = "1")
        Long prestacionId,

        @Schema(description = "ID del profesional elegido, o null para cualquier profesional", example = "2")
        Long profesionalId,

        @NotNull(message = "El horario de inicio es obligatorio")
        @Schema(description = "Horario de inicio del turno en formato ISO con offset del consultorio (-03:00)", example = "2026-12-01T09:00:00-03:00")
        OffsetDateTime inicio,

        @NotNull(message = "Los datos del paciente son obligatorios")
        @Valid
        @Schema(description = "Datos personales del paciente")
        PacienteSolicitud paciente,

        @NotNull(message = ServicioDePacientes.AVISO_NO_ACEPTADO)
        @AssertTrue(message = ServicioDePacientes.AVISO_NO_ACEPTADO)
        @Schema(description = "Aceptación obligatoria del aviso de privacidad", example = "true")
        Boolean aceptaAvisoPrivacidad) {
}

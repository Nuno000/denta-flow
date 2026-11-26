package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.aplicacion.turnos.PedidoDeCargaManual;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Cuerpo de {@code POST /api/turnos} y {@code POST /api/turnos/sobreturnos} (C-12, design D1). No tiene campo
 * {@code esSobreturno}: un campo desconocido se ignora, así que la carga común nunca crea sobreturnos. El formato fino
 * del DNI, el teléfono y el email lo valida el dominio ({@code Dni}, {@code Paciente}) y sale como 400.
 */
public record SolicitudDeCargaManual(
        @Schema(description = "Datos de contacto del paciente; se registra o actualiza por DNI") @NotNull @Valid
        PacienteDeCarga paciente,
        @Schema(description = "Prestación a realizar", example = "4") @NotNull @Positive Long prestacionId,
        @Schema(description = "Profesional; debe realizar la prestación", example = "10") @NotNull @Positive
        Long profesionalId,
        @Schema(description = "Sillón", example = "1") @NotNull @Positive Long sillonId,
        @Schema(description = "Inicio con su desplazamiento horario, sin segundos", example = "2026-12-01T09:00:00-03:00")
        @NotNull OffsetDateTime inicio) {

    /** Datos del paciente tal como los carga el personal; el email es obligatorio (DD-12). */
    public record PacienteDeCarga(
            @Schema(description = "DNI, con o sin puntos", example = "30123456") @NotBlank @Size(max = 20) String dni,
            @NotBlank @Size(max = 100) String nombre,
            @NotBlank @Size(max = 100) String apellido,
            @Schema(example = "+541155550000") @NotBlank @Size(max = 30) String telefono,
            @NotBlank @Size(max = 254) String email) {

        /** Evita imprimir datos personales en logs o mensajes de error. */
        @Override
        public String toString() {
            return "PacienteDeCarga[...]";
        }
    }

    /** Pasa a la capa de aplicación; un inicio con segundos se rechaza (los turnos arrancan en minutos exactos). */
    public PedidoDeCargaManual aPedido() {
        if (inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new ValidacionDeDominioException("El inicio del turno debe ser en minutos exactos, sin segundos");
        }
        return new PedidoDeCargaManual(paciente.dni(), paciente.nombre(), paciente.apellido(), paciente.telefono(),
                paciente.email(), prestacionId, profesionalId, sillonId, inicio.toInstant());
    }
}

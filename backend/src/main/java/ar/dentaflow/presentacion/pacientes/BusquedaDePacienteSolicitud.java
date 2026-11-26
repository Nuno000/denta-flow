package ar.dentaflow.presentacion.pacientes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpo de {@code POST /api/pacientes/busqueda} (C-12). El formato del DNI lo valida {@code Dni.normalizar}. */
public record BusquedaDePacienteSolicitud(
        @Schema(description = "DNI con o sin puntos", example = "30.123.456") @NotBlank @Size(max = 20) String dni) {

    /** Evita imprimir el DNI en logs o mensajes de error. */
    @Override
    public String toString() {
        return "BusquedaDePacienteSolicitud[...]";
    }
}

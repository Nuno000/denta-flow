package ar.dentaflow.presentacion.recursos;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/** Conjunto completo de prestaciones que realiza un profesional; reemplaza al anterior (design D5). */
public record AsignarPrestacionesSolicitud(
        @NotNull(message = "La lista de prestaciones es obligatoria")
        List<@NotNull(message = "El id de la prestación es obligatorio") Long> prestacionIds) {
}

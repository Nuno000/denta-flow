package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.reservas.ReservaConfirmada;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record ReservaRespuesta(
        @Schema(description = "Horario de inicio del turno con offset -03:00", example = "2026-12-01T09:00:00-03:00")
        OffsetDateTime inicio,

        @Schema(description = "Horario de finalización del turno con offset -03:00", example = "2026-12-01T09:30:00-03:00")
        OffsetDateTime fin,

        @Schema(description = "Nombre de la prestación", example = "Limpieza dental")
        String prestacion,

        @Schema(description = "Nombre y apellido del profesional asignado", example = "Laura Ejemplo")
        String profesional,

        @Schema(description = "Estado inicial del turno", example = "pendiente")
        String estado) {

    public static ReservaRespuesta de(ReservaConfirmada confirmada) {
        return new ReservaRespuesta(
                confirmada.inicio().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                confirmada.fin().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                confirmada.prestacion(),
                confirmada.profesional(),
                confirmada.estado().codigo());
    }
}

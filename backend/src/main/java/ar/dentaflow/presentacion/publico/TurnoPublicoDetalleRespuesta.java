package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.turnos.TurnoPublicoDetalle;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record TurnoPublicoDetalleRespuesta(
        @Schema(description = "Situación del enlace del turno", example = "VIGENTE")
        String situacion,

        @Schema(description = "Horario de inicio del turno con offset -03:00", example = "2026-12-05T10:00:00-03:00")
        OffsetDateTime inicio,

        @Schema(description = "Horario de fin del turno con offset -03:00", example = "2026-12-05T10:30:00-03:00")
        OffsetDateTime fin,

        @Schema(description = "Nombre de la prestación", example = "Limpieza dental")
        String prestacion,

        @Schema(description = "Nombre del profesional asignado", example = "Dra. Laura Ejemplo")
        String profesional,

        @Schema(description = "Estado actual del turno", example = "pendiente")
        String estado,

        @Schema(description = "Indica si el paciente puede modificar o cancelar el turno dentro de la anticipación permitida", example = "true")
        boolean puedeModificar) {

    public static TurnoPublicoDetalleRespuesta de(TurnoPublicoDetalle detalle) {
        return new TurnoPublicoDetalleRespuesta(
                detalle.situacion().name(),
                detalle.inicio().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                detalle.fin().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                detalle.prestacion(),
                detalle.profesional(),
                detalle.estado(),
                detalle.puedeModificar());
    }
}

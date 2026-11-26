package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import ar.dentaflow.dominio.disponibilidad.AmbitoBloqueo;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.disponibilidad.MotivoBloqueo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.OffsetDateTime;

/** Bloqueo con su franja expresada en la zona del consultorio. */
public record BloqueoRespuesta(
        Long id,
        AmbitoBloqueo ambito,
        Long profesionalId,
        Long sillonId,
        @Schema(example = "2026-12-24T08:00:00-03:00") OffsetDateTime inicio,
        @Schema(example = "2026-12-24T13:00:00-03:00") OffsetDateTime fin,
        MotivoBloqueo motivo,
        String detalle,
        @Schema(description = "Usuario que grabó la última versión") Long modificadoPor,
        @Schema(description = "Instante UTC de la última versión") Instant modificadoEn) {

    static BloqueoRespuesta de(Bloqueo bloqueo) {
        return new BloqueoRespuesta(bloqueo.getId(), bloqueo.getAmbito(), bloqueo.getProfesionalId(),
                bloqueo.getSillonId(), enZonaDelConsultorio(bloqueo.getInicio()),
                enZonaDelConsultorio(bloqueo.getFin()), bloqueo.getMotivo(), bloqueo.getDetalle(),
                bloqueo.getModificadoPor(), bloqueo.getModificadoEn());
    }

    static OffsetDateTime enZonaDelConsultorio(Instant instante) {
        return instante.atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime();
    }
}

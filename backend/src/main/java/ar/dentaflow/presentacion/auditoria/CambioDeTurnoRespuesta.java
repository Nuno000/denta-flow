package ar.dentaflow.presentacion.auditoria;

import ar.dentaflow.aplicacion.auditoria.EntradaDeHistorial;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/** Registro de auditoría expuesto solo al administrador (C-13, US-025; sin datos personales del paciente). */
public record CambioDeTurnoRespuesta(
        Long id,
        @Schema(description = "Instante del cambio en UTC (ISO-8601)") Instant ocurridoEn,
        @Schema(allowableValues = {"usuario", "paciente", "sistema"}) String actorTipo,
        @Schema(description = "Email del usuario interno cuando actorTipo es usuario; null en paciente o sistema")
        String actorEmail,
        @Schema(allowableValues = {"creacion", "confirmacion", "cancelacion", "reprogramacion", "cambio_estado"})
        String accion,
        Map<String, String> valorAnterior,
        Map<String, String> valorNuevo) {

    static CambioDeTurnoRespuesta de(EntradaDeHistorial entrada) {
        return new CambioDeTurnoRespuesta(
                entrada.id(),
                entrada.ocurridoEn(),
                entrada.actorTipo().codigo(),
                entrada.actorEmail(),
                entrada.accion().codigo(),
                entrada.valorAnterior(),
                entrada.valorNuevo());
    }
}

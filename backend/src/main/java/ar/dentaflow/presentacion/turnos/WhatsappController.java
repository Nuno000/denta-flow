package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.aplicacion.notificaciones.MensajeWhatsapp;
import ar.dentaflow.aplicacion.notificaciones.ServicioDeMensajesWhatsapp;
import ar.dentaflow.infraestructura.seguridad.guardas.AdministradorORecepcion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mensaje de WhatsApp prellenado de un turno (C-21, US-032, RN-NO-04; spec mensajes-whatsapp, design D1). Solo
 * administrador y recepción. El enlace se arma acá, turno por turno, para que la agenda no exponga el teléfono ni
 * el token del paciente. No envía nada: recepción abre el enlace y envía desde su WhatsApp.
 */
@RestController
@RequestMapping("/api/turnos")
@Tag(name = "Turnos", description = "Acciones operativas sobre turnos del consultorio")
@SecurityRequirement(name = "bearerAuth")
public class WhatsappController {

    private final ServicioDeMensajesWhatsapp servicio;

    public WhatsappController(ServicioDeMensajesWhatsapp servicio) {
        this.servicio = servicio;
    }

    @AdministradorORecepcion
    @GetMapping("/{id}/whatsapp")
    @Operation(summary = "Mensaje de WhatsApp prellenado de un turno",
            description = "Arma el mensaje con la plantilla whatsapp-plantilla y el enlace wa.me con el teléfono del "
                    + "paciente normalizado al formato argentino. Si el teléfono no es utilizable, enlace es null. "
                    + "No envía el mensaje ni cambia el turno.")
    @ApiResponse(responseCode = "200", description = "Mensaje y enlace (o enlace null)")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador ni recepción")
    @ApiResponse(responseCode = "404", description = "El turno no existe")
    @ApiResponse(responseCode = "409", description = "El turno no está pendiente ni confirmado")
    public ResponseEntity<MensajeWhatsappRespuesta> mensaje(@PathVariable long id) {
        MensajeWhatsapp mensaje = servicio.prepararPara(id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new MensajeWhatsappRespuesta(mensaje.mensaje(), mensaje.enlace()));
    }

    public record MensajeWhatsappRespuesta(
            @Schema(description = "Mensaje armado con la plantilla del consultorio") String mensaje,
            @Schema(description = "Enlace https://wa.me/<teléfono>?text=<mensaje>, o null sin teléfono utilizable",
                    nullable = true) String enlace) {
    }
}

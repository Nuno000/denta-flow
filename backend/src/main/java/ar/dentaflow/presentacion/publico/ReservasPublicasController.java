package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.reservas.ReservaConfirmada;
import ar.dentaflow.aplicacion.reservas.ServicioDeReservaOnline;
import ar.dentaflow.aplicacion.reservas.SolicitudDeReservaOnline;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint público de reserva online (C-16 design D3): crea un turno en estado pendiente a partir de un hueco
 * disponible, sin autenticación previa del paciente y respetando todas las reglas anti-abuso y privacidad.
 */
@RestController
@RequestMapping("/api/publico/reservas")
@Tag(name = "Reserva pública", description = "Endpoints sin autenticación para el paciente")
public class ReservasPublicasController {

    private final ServicioDeReservaOnline servicio;

    public ReservasPublicasController(ServicioDeReservaOnline servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Reservar un turno online", description = "Crea un turno en estado pendiente para el paciente")
    @ApiResponse(responseCode = "201", description = "Turno reservado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o aviso de privacidad no aceptado")
    @ApiResponse(responseCode = "404", description = "Prestación o profesional inexistente")
    @ApiResponse(responseCode = "409", description = "El horario elegido ya no está disponible")
    @ApiResponse(responseCode = "429", description = "Demasiados pedidos de reserva desde este origen")
    public ResponseEntity<ReservaRespuesta> reservar(
            @Valid @RequestBody ReservaSolicitud solicitud,
            HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        ReservaConfirmada confirmada = servicio.reservar(new SolicitudDeReservaOnline(
                solicitud.prestacionId(),
                solicitud.profesionalId(),
                solicitud.inicio().toInstant(),
                solicitud.paciente().dni(),
                solicitud.paciente().nombre(),
                solicitud.paciente().apellido(),
                solicitud.paciente().telefono(),
                solicitud.paciente().email(),
                solicitud.aceptaAvisoPrivacidad(),
                ip != null ? ip : "desconocido"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .cacheControl(CacheControl.noStore())
                .body(ReservaRespuesta.de(confirmada));
    }
}

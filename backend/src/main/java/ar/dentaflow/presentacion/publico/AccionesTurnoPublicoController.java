package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.reservas.LimitadorDeReservasPorOrigen;
import ar.dentaflow.aplicacion.turnos.ServicioDeGestionTurnoPaciente;
import ar.dentaflow.aplicacion.turnos.TurnoPublicoDetalle;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos para la gestión autónoma del turno por parte del paciente mediante su token no adivinable
 * (C-17 design D1, D2, D4): consultar resumen, confirmar con un toque, cancelar con anticipación mínima, consultar huecos
 * y reprogramar atómicamente, sin exponer datos personales (RN-PR-03). Las acciones (POST) consumen el límite por IP
 * compartido con la reserva pública (Q5); las lecturas no, para que el paciente pueda recorrer días al reprogramar.
 */
@RestController
@RequestMapping("/api/publico/turnos")
@Tag(name = "Gestión pública de turnos", description = "Endpoints para que el paciente confirme, cancele o reprograme su turno")
public class AccionesTurnoPublicoController {

    private final ServicioDeGestionTurnoPaciente servicio;
    private final LimitadorDeReservasPorOrigen limitador;

    public AccionesTurnoPublicoController(ServicioDeGestionTurnoPaciente servicio,
            LimitadorDeReservasPorOrigen limitador) {
        this.servicio = servicio;
        this.limitador = limitador;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Consultar resumen y estado del turno", description = "Devuelve los datos del turno sin exponer datos personales")
    @ApiResponse(responseCode = "200", description = "Detalle del turno")
    @ApiResponse(responseCode = "404", description = "Enlace no válido o expirado")
    public ResponseEntity<TurnoPublicoDetalleRespuesta> consultar(
            @PathVariable String token) {
        TurnoPublicoDetalle detalle = servicio.consultarPorToken(token);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TurnoPublicoDetalleRespuesta.de(detalle));
    }

    @PostMapping("/{token}/confirmar")
    @Operation(summary = "Confirmar asistencia", description = "Confirma el turno con un toque de forma idempotente")
    @ApiResponse(responseCode = "200", description = "Turno confirmado")
    @ApiResponse(responseCode = "404", description = "Enlace no válido o expirado")
    @ApiResponse(responseCode = "409", description = "El turno ya fue cancelado o ha vencido")
    @ApiResponse(responseCode = "429", description = "Demasiados pedidos desde este origen")
    public ResponseEntity<TurnoPublicoDetalleRespuesta> confirmar(
            @PathVariable String token,
            HttpServletRequest request) {
        limitar(request);
        TurnoPublicoDetalle detalle = servicio.confirmar(token);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TurnoPublicoDetalleRespuesta.de(detalle));
    }

    @PostMapping("/{token}/cancelar")
    @Operation(summary = "Cancelar turno", description = "Cancela el turno y libera los recursos asignados dentro de la anticipación mínima")
    @ApiResponse(responseCode = "200", description = "Turno cancelado")
    @ApiResponse(responseCode = "404", description = "Enlace no válido o expirado")
    @ApiResponse(responseCode = "409", description = "Cancelación fuera de término o turno no modificable")
    @ApiResponse(responseCode = "429", description = "Demasiados pedidos desde este origen")
    public ResponseEntity<TurnoPublicoDetalleRespuesta> cancelar(
            @PathVariable String token,
            HttpServletRequest request) {
        limitar(request);
        TurnoPublicoDetalle detalle = servicio.cancelar(token);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TurnoPublicoDetalleRespuesta.de(detalle));
    }

    @GetMapping("/{token}/huecos")
    @Operation(summary = "Consultar huecos para reprogramar", description = "Devuelve los horarios disponibles excluyendo el propio turno")
    @ApiResponse(responseCode = "200", description = "Lista ordenada de huecos disponibles")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos")
    @ApiResponse(responseCode = "404", description = "Enlace no válido o expirado")
    @ApiResponse(responseCode = "409", description = "Fuera de término o turno no vigente")
    public ResponseEntity<List<HuecoPublicoRespuesta>> huecos(
            @PathVariable String token,
            @RequestParam String desde,
            @RequestParam String hasta) {
        LocalDate fechaDesde = parsearFecha(desde, "desde");
        LocalDate fechaHasta = parsearFecha(hasta, "hasta");

        List<HuecoPublicoRespuesta> huecos = servicio.consultarHuecosParaReprogramar(token, fechaDesde, fechaHasta)
                .stream()
                .map(h -> new HuecoPublicoRespuesta(
                        h.inicio().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                        h.fin().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                        null))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(huecos);
    }

    @PostMapping("/{token}/reprogramar")
    @Operation(summary = "Reprogramar turno", description = "Mueve el turno a un nuevo horario de forma atómica")
    @ApiResponse(responseCode = "200", description = "Turno reprogramado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de reprogramación inválidos")
    @ApiResponse(responseCode = "404", description = "Enlace no válido o expirado")
    @ApiResponse(responseCode = "409", description = "Fuera de término o horario ya tomado")
    @ApiResponse(responseCode = "429", description = "Demasiados pedidos desde este origen")
    public ResponseEntity<TurnoPublicoDetalleRespuesta> reprogramar(
            @PathVariable String token,
            @Valid @RequestBody ReprogramarSolicitud solicitud,
            HttpServletRequest request) {
        limitar(request);
        TurnoPublicoDetalle detalle = servicio.reprogramar(token, solicitud.inicio().toInstant());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(TurnoPublicoDetalleRespuesta.de(detalle));
    }

    private void limitar(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        limitador.registrarPedido(ip != null ? ip : "desconocido");
    }

    private static LocalDate parsearFecha(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new ValidacionDeDominioException("La fecha '" + campo + "' debe tener formato AAAA-MM-DD");
        }
    }
}

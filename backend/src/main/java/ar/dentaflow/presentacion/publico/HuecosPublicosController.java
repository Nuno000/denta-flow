package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.agenda.ConsultaDeHuecos;
import ar.dentaflow.aplicacion.agenda.MotorDeDisponibilidad;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta pública de huecos disponibles para la reserva online (C-10, design D7). No expone
 * datos personales ni sillones.
 */
@RestController
@RequestMapping("/api/publico/huecos")
@Tag(name = "Reserva pública", description = "Consulta de huecos y reserva online sin registro")
public class HuecosPublicosController {

    private final MotorDeDisponibilidad motor;

    public HuecosPublicosController(MotorDeDisponibilidad motor) {
        this.motor = motor;
    }

    @GetMapping
    @Operation(summary = "Listar huecos disponibles", description = "Devuelve los horarios disponibles para una prestación y un rango de fechas del consultorio.")
    @ApiResponse(responseCode = "200", description = "Lista ordenada de huecos disponibles")
    @ApiResponse(responseCode = "400", description = "Parámetros inválidos o rango fuera de límite")
    @ApiResponse(responseCode = "404", description = "Prestación o profesional inexistente")
    public ResponseEntity<List<HuecoPublicoRespuesta>> listar(
            @RequestParam Long prestacionId,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam String desde,
            @RequestParam String hasta) {
        LocalDate fechaDesde = parsearFecha(desde, "desde");
        LocalDate fechaHasta = parsearFecha(hasta, "hasta");

        List<HuecoPublicoRespuesta> respuesta = motor
                .buscarHuecos(new ConsultaDeHuecos(prestacionId, profesionalId, fechaDesde, fechaHasta, null))
                .stream()
                .map(h -> new HuecoPublicoRespuesta(
                        h.inicio().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                        h.fin().atZone(ZonaHorariaConsultorio.ZONA).toOffsetDateTime(),
                        profesionalId))
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }

    private static LocalDate parsearFecha(String valor, String campo) {
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            throw new ValidacionDeDominioException("La fecha '" + campo + "' debe tener formato AAAA-MM-DD");
        }
    }
}

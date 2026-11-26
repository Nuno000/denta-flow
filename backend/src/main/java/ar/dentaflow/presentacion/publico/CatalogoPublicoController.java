package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.reservas.CatalogoPublico;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo para la reserva online (C-16 design D3): prestaciones activas y profesionales de una prestación. Ruta
 * pública, sin datos internos (emails, matrículas, sillones) y sin caché, porque el administrador puede
 * desactivar un recurso en cualquier momento.
 */
@RestController
@RequestMapping("/api/publico")
@Tag(name = "Reserva pública", description = "Endpoints sin autenticación para el paciente")
public class CatalogoPublicoController {

    private final CatalogoPublico catalogo;

    public CatalogoPublicoController(CatalogoPublico catalogo) {
        this.catalogo = catalogo;
    }

    @GetMapping("/prestaciones")
    @Operation(summary = "Prestaciones que se pueden reservar", description = "Solo activas, ordenadas por nombre")
    @ApiResponse(responseCode = "200", description = "Prestaciones con id, nombre y duración en minutos")
    public ResponseEntity<List<PrestacionPublica>> prestaciones() {
        return sinCache(catalogo.prestaciones().stream().map(PrestacionPublica::de).toList());
    }

    @GetMapping("/profesionales")
    @Operation(summary = "Profesionales de una prestación", description = "Solo activos que realizan la prestación")
    @ApiResponse(responseCode = "200", description = "Profesionales con id, nombre y apellido")
    @ApiResponse(responseCode = "404", description = "La prestación no existe o está inactiva")
    public ResponseEntity<List<ProfesionalPublico>> profesionales(@RequestParam Long prestacionId) {
        return sinCache(catalogo.profesionalesDe(prestacionId).stream().map(ProfesionalPublico::de).toList());
    }

    private static <T> ResponseEntity<T> sinCache(T cuerpo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuerpo);
    }
}

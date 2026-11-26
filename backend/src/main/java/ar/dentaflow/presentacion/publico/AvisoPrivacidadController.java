package ar.dentaflow.presentacion.publico;

import ar.dentaflow.aplicacion.configuracion.ParametrosDelConsultorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aviso de privacidad que el paciente acepta al reservar (C-15, design D5). Ruta pública: vive bajo
 * {@code /api/publico/**}. Sin caché, para que un cambio del administrador se vea al instante.
 */
@RestController
@RequestMapping("/api/publico/aviso-privacidad")
@Tag(name = "Reserva pública", description = "Endpoints sin autenticación para el paciente")
public class AvisoPrivacidadController {

    private final ParametrosDelConsultorio parametros;

    public AvisoPrivacidadController(ParametrosDelConsultorio parametros) {
        this.parametros = parametros;
    }

    @GetMapping
    @Operation(summary = "Aviso de privacidad vigente",
            description = "Texto del aviso y leyenda de la casilla de aceptación (Ley 25.326)")
    @ApiResponse(responseCode = "200", description = "Texto y leyenda del aviso")
    public ResponseEntity<AvisoPrivacidadRespuesta> obtener() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(AvisoPrivacidadRespuesta.de(parametros.avisoPrivacidad()));
    }
}

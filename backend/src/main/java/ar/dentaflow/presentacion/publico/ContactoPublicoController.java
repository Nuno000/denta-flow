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
 * Contacto del consultorio para cuando el paciente no puede reservar online: sin huecos o con el horario tomado
 * (C-16 design D3, Flujo 1). Ruta pública; sin caché para que un cambio del administrador se vea al instante.
 */
@RestController
@RequestMapping("/api/publico/contacto")
@Tag(name = "Reserva pública", description = "Endpoints sin autenticación para el paciente")
public class ContactoPublicoController {

    private final ParametrosDelConsultorio parametros;

    public ContactoPublicoController(ParametrosDelConsultorio parametros) {
        this.parametros = parametros;
    }

    @GetMapping
    @Operation(summary = "Contacto del consultorio", description = "Texto editable por el administrador")
    @ApiResponse(responseCode = "200", description = "Contacto vigente")
    public ResponseEntity<ContactoRespuesta> obtener() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ContactoRespuesta(parametros.contactoConsultorio()));
    }
}

package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.aplicacion.recursos.ServicioDeSillones;
import ar.dentaflow.infraestructura.seguridad.guardas.PersonalInterno;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sillones del consultorio (US-001, spec gestion-recursos, design D3): lectura para el personal interno y
 * modificación solo para el administrador. Sin borrado: la baja es la desactivación.
 */
@RestController
@RequestMapping("/api/sillones")
@Tag(name = "Sillones", description = "Sillones del consultorio: consulta del personal y gestión del administrador")
@SecurityRequirement(name = "bearerAuth")
public class SillonesController {

    private final ServicioDeSillones servicio;

    public SillonesController(ServicioDeSillones servicio) {
        this.servicio = servicio;
    }

    @PersonalInterno
    @GetMapping
    @Operation(summary = "Listar sillones", description = "Activos e inactivos, por id")
    public List<SillonRespuesta> listar() {
        return servicio.listar().stream().map(SillonRespuesta::de).toList();
    }

    @PersonalInterno
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un sillón")
    @ApiResponse(responseCode = "404", description = "El sillón no existe")
    public SillonRespuesta obtener(@PathVariable Long id) {
        return SillonRespuesta.de(servicio.obtener(id));
    }

    @SoloAdministrador
    @PostMapping
    @Operation(summary = "Dar de alta un sillón")
    @ApiResponse(responseCode = "201", description = "Sillón creado y activo")
    @ApiResponse(responseCode = "400", description = "Nombre faltante o demasiado largo")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "409", description = "Ya existe un sillón con ese nombre")
    public ResponseEntity<SillonRespuesta> crear(@Valid @RequestBody SillonSolicitud solicitud) {
        SillonRespuesta creado = SillonRespuesta.de(servicio.crear(solicitud.nombre()));
        return ResponseEntity.created(URI.create("/api/sillones/" + creado.id())).body(creado);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    @Operation(summary = "Renombrar un sillón")
    @ApiResponse(responseCode = "400", description = "Nombre faltante o demasiado largo")
    @ApiResponse(responseCode = "404", description = "El sillón no existe")
    @ApiResponse(responseCode = "409", description = "Ya existe otro sillón con ese nombre")
    public SillonRespuesta renombrar(@PathVariable Long id, @Valid @RequestBody SillonSolicitud solicitud) {
        return SillonRespuesta.de(servicio.renombrar(id, solicitud.nombre()));
    }

    @SoloAdministrador
    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar un sillón", description = "Idempotente; conserva sus datos")
    @ApiResponse(responseCode = "404", description = "El sillón no existe")
    public SillonRespuesta desactivar(@PathVariable Long id) {
        return SillonRespuesta.de(servicio.desactivar(id));
    }

    @SoloAdministrador
    @PostMapping("/{id}/activar")
    @Operation(summary = "Reactivar un sillón", description = "Idempotente")
    @ApiResponse(responseCode = "404", description = "El sillón no existe")
    public SillonRespuesta activar(@PathVariable Long id) {
        return SillonRespuesta.de(servicio.activar(id));
    }
}

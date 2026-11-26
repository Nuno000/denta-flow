package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.aplicacion.recursos.ServicioDePrestaciones;
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
 * Prestaciones y sus duraciones (US-001, RN-TU-03, spec gestion-recursos, design D3): lectura para el
 * personal interno y modificación solo para el administrador. Sin borrado: la baja es la desactivación.
 */
@RestController
@RequestMapping("/api/prestaciones")
@Tag(name = "Prestaciones", description = "Prestaciones y duraciones: consulta del personal y gestión del administrador")
@SecurityRequirement(name = "bearerAuth")
public class PrestacionesController {

    private final ServicioDePrestaciones servicio;

    public PrestacionesController(ServicioDePrestaciones servicio) {
        this.servicio = servicio;
    }

    @PersonalInterno
    @GetMapping
    @Operation(summary = "Listar prestaciones", description = "Activas e inactivas, por nombre")
    public List<PrestacionRespuesta> listar() {
        return servicio.listar().stream().map(PrestacionRespuesta::de).toList();
    }

    @PersonalInterno
    @GetMapping("/{id}")
    @Operation(summary = "Consultar una prestación")
    @ApiResponse(responseCode = "404", description = "La prestación no existe")
    public PrestacionRespuesta obtener(@PathVariable Long id) {
        return PrestacionRespuesta.de(servicio.obtener(id));
    }

    @SoloAdministrador
    @PostMapping
    @Operation(summary = "Dar de alta una prestación")
    @ApiResponse(responseCode = "201", description = "Prestación creada y activa")
    @ApiResponse(responseCode = "400", description = "Nombre faltante o duración faltante, cero o negativa")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "409", description = "Ya existe una prestación con ese nombre")
    public ResponseEntity<PrestacionRespuesta> crear(@Valid @RequestBody PrestacionSolicitud solicitud) {
        PrestacionRespuesta creada = PrestacionRespuesta.de(
                servicio.crear(solicitud.nombre(), solicitud.duracionMinutos()));
        return ResponseEntity.created(URI.create("/api/prestaciones/" + creada.id())).body(creada);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    @Operation(summary = "Editar nombre y duración",
            description = "La duración nueva no cambia la de los turnos ya creados")
    @ApiResponse(responseCode = "400", description = "Nombre faltante o duración faltante, cero o negativa")
    @ApiResponse(responseCode = "404", description = "La prestación no existe")
    @ApiResponse(responseCode = "409", description = "Ya existe otra prestación con ese nombre")
    public PrestacionRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody PrestacionSolicitud solicitud) {
        return PrestacionRespuesta.de(servicio.actualizar(id, solicitud.nombre(), solicitud.duracionMinutos()));
    }

    @SoloAdministrador
    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar una prestación",
            description = "Idempotente; los profesionales que la realizan la conservan asociada")
    @ApiResponse(responseCode = "404", description = "La prestación no existe")
    public PrestacionRespuesta desactivar(@PathVariable Long id) {
        return PrestacionRespuesta.de(servicio.desactivar(id));
    }

    @SoloAdministrador
    @PostMapping("/{id}/activar")
    @Operation(summary = "Reactivar una prestación", description = "Idempotente")
    @ApiResponse(responseCode = "404", description = "La prestación no existe")
    public PrestacionRespuesta activar(@PathVariable Long id) {
        return PrestacionRespuesta.de(servicio.activar(id));
    }
}

package ar.dentaflow.presentacion.recursos;

import ar.dentaflow.aplicacion.recursos.ServicioDeProfesionales;
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
 * Profesionales y las prestaciones que realizan (US-001, spec gestion-recursos, design D3 y D5): lectura para
 * el personal interno y modificación solo para el administrador. Sin borrado: la baja es la desactivación,
 * que conserva las prestaciones.
 */
@RestController
@RequestMapping("/api/profesionales")
@Tag(name = "Profesionales", description = "Profesionales: consulta del personal y gestión del administrador")
@SecurityRequirement(name = "bearerAuth")
public class ProfesionalesController {

    private final ServicioDeProfesionales servicio;

    public ProfesionalesController(ServicioDeProfesionales servicio) {
        this.servicio = servicio;
    }

    @PersonalInterno
    @GetMapping
    @Operation(summary = "Listar profesionales",
            description = "Activos e inactivos, por apellido y nombre, con los ids de sus prestaciones")
    public List<ProfesionalRespuesta> listar() {
        return servicio.listar().stream().map(ProfesionalRespuesta::de).toList();
    }

    @PersonalInterno
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un profesional")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    public ProfesionalRespuesta obtener(@PathVariable Long id) {
        return ProfesionalRespuesta.de(servicio.obtener(id));
    }

    @SoloAdministrador
    @PostMapping
    @Operation(summary = "Dar de alta un profesional", description = "Nace activo y sin prestaciones")
    @ApiResponse(responseCode = "201", description = "Profesional creado")
    @ApiResponse(responseCode = "400", description = "Dato faltante, demasiado largo o email mal formado")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "409", description = "Email o matrícula de otro profesional")
    public ResponseEntity<ProfesionalRespuesta> crear(@Valid @RequestBody ProfesionalSolicitud solicitud) {
        ProfesionalRespuesta creado = ProfesionalRespuesta.de(servicio.crear(solicitud.nombre(), solicitud.apellido(),
                solicitud.matricula(), solicitud.email()));
        return ResponseEntity.created(URI.create("/api/profesionales/" + creado.id())).body(creado);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    @Operation(summary = "Editar los datos de un profesional", description = "No cambia su estado ni sus prestaciones")
    @ApiResponse(responseCode = "400", description = "Dato faltante, demasiado largo o email mal formado")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    @ApiResponse(responseCode = "409", description = "Email o matrícula de otro profesional")
    public ProfesionalRespuesta actualizar(@PathVariable Long id, @Valid @RequestBody ProfesionalSolicitud solicitud) {
        return ProfesionalRespuesta.de(servicio.actualizar(id, solicitud.nombre(), solicitud.apellido(),
                solicitud.matricula(), solicitud.email()));
    }

    @SoloAdministrador
    @PutMapping("/{id}/prestaciones")
    @Operation(summary = "Reemplazar las prestaciones que realiza",
            description = "Las agregadas deben existir y estar activas; las inactivas que ya tenía se pueden conservar")
    @ApiResponse(responseCode = "400", description = "Prestación inexistente o inactiva agregada; no cambia nada")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    public ProfesionalRespuesta asignarPrestaciones(@PathVariable Long id,
            @Valid @RequestBody AsignarPrestacionesSolicitud solicitud) {
        return ProfesionalRespuesta.de(servicio.asignarPrestaciones(id, solicitud.prestacionIds()));
    }

    @SoloAdministrador
    @PostMapping("/{id}/desactivar")
    @Operation(summary = "Desactivar un profesional", description = "Idempotente; conserva datos y prestaciones")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    public ProfesionalRespuesta desactivar(@PathVariable Long id) {
        return ProfesionalRespuesta.de(servicio.desactivar(id));
    }

    @SoloAdministrador
    @PostMapping("/{id}/activar")
    @Operation(summary = "Reactivar un profesional", description = "Idempotente")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    public ProfesionalRespuesta activar(@PathVariable Long id) {
        return ProfesionalRespuesta.de(servicio.activar(id));
    }
}

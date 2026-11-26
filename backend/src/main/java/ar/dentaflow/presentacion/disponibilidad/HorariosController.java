package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.aplicacion.disponibilidad.ServicioDeHorarios;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Horario de atención semanal por profesional (spec horarios-atencion, design D8). El administrador
 * crea, edita y borra; recepción lee todo; el rol profesional lee solo su propio horario.
 */
@RestController
@Tag(name = "Horarios", description = "Horario de atención semanal de cada profesional, en hora del consultorio")
@SecurityRequirement(name = "bearerAuth")
public class HorariosController {

    private final ServicioDeHorarios servicio;

    public HorariosController(ServicioDeHorarios servicio) {
        this.servicio = servicio;
    }

    @PersonalInterno
    @GetMapping("/api/profesionales/{profesionalId}/horarios")
    @Operation(summary = "Consultar el horario de un profesional",
            description = "Rangos ordenados por día (lunes primero) y hora de inicio")
    @ApiResponse(responseCode = "200", description = "Rangos del profesional")
    @ApiResponse(responseCode = "403", description = "Rol profesional consultando a otro profesional")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    public List<HorarioRespuesta> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long profesionalId) {
        AccesoPorProfesional.verificarProfesional(usuario, profesionalId);
        return servicio.listarDelProfesional(profesionalId).stream().map(HorarioRespuesta::de).toList();
    }

    @SoloAdministrador
    @PostMapping("/api/profesionales/{profesionalId}/horarios")
    @Operation(summary = "Agregar un rango de atención",
            description = "Sin superposición con otro rango del mismo profesional y día; los contiguos se permiten")
    @ApiResponse(responseCode = "201", description = "Rango creado")
    @ApiResponse(responseCode = "400", description = "Datos faltantes, inicio no anterior al fin u horas con segundos")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El profesional no existe")
    @ApiResponse(responseCode = "409", description = "Se superpone con otro rango o el profesional está inactivo")
    public ResponseEntity<HorarioRespuesta> crear(@PathVariable Long profesionalId,
            @Valid @RequestBody HorarioSolicitud solicitud) {
        HorarioRespuesta creado = HorarioRespuesta.de(servicio.crear(profesionalId, solicitud.dia().aDayOfWeek(),
                solicitud.inicio(), solicitud.fin()));
        return ResponseEntity.created(URI.create("/api/horarios/" + creado.id())).body(creado);
    }

    @SoloAdministrador
    @PutMapping("/api/horarios/{id}")
    @Operation(summary = "Editar un rango de atención")
    @ApiResponse(responseCode = "200", description = "Rango actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El rango no existe")
    @ApiResponse(responseCode = "409", description = "Se superpone con otro rango del mismo día")
    public HorarioRespuesta editar(@PathVariable Long id, @Valid @RequestBody HorarioSolicitud solicitud) {
        return HorarioRespuesta.de(servicio.editar(id, solicitud.dia().aDayOfWeek(), solicitud.inicio(),
                solicitud.fin()));
    }

    @SoloAdministrador
    @DeleteMapping("/api/horarios/{id}")
    @Operation(summary = "Borrar un rango de atención")
    @ApiResponse(responseCode = "204", description = "Rango borrado")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El rango no existe")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

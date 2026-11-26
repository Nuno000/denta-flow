package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.aplicacion.disponibilidad.ServicioDeBloqueos;
import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.PersonalInterno;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bloqueos de agenda (spec bloqueos-agenda, design D8). El administrador crea, edita y borra;
 * recepción lee todo; el rol profesional lee solo los suyos y los del consultorio.
 */
@RestController
@RequestMapping("/api/bloqueos")
@Tag(name = "Bloqueos", description = "Vacaciones, feriados y ausencias de un profesional, un sillón o el consultorio")
@SecurityRequirement(name = "bearerAuth")
public class BloqueosController {

    private final ServicioDeBloqueos servicio;

    public BloqueosController(ServicioDeBloqueos servicio) {
        this.servicio = servicio;
    }

    @PersonalInterno
    @GetMapping
    @Operation(summary = "Consultar bloqueos por rango",
            description = "Bloqueos que se superponen con [desde, hasta), ordenados por inicio. Rango máximo de "
                    + "366 días. Con rol profesional se devuelven solo los suyos y los del consultorio, "
                    + "sin importar los filtros")
    @ApiResponse(responseCode = "200", description = "Bloqueos del rango")
    @ApiResponse(responseCode = "400",
            description = "Falta desde o hasta, desde no es anterior a hasta o el rango supera 366 días")
    @ApiResponse(responseCode = "403", description = "Profesional sin vínculo")
    public List<BloqueoRespuesta> listar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(example = "2026-12-01T00:00:00-03:00") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @Parameter(example = "2027-01-01T00:00:00-03:00") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId) {
        Instant inicio = aInstante(desde);
        Instant fin = aInstante(hasta);
        Optional<Long> propio = AccesoPorProfesional.profesionalPropio(usuario);
        List<Bloqueo> encontrados = propio.isPresent()
                ? servicio.listarQueAfectanAProfesional(propio.get(), inicio, fin)
                : servicio.listar(inicio, fin, profesionalId, sillonId);
        return encontrados.stream().map(BloqueoRespuesta::de).toList();
    }

    @PersonalInterno
    @GetMapping("/{id}")
    @Operation(summary = "Consultar un bloqueo")
    @ApiResponse(responseCode = "200", description = "Bloqueo encontrado")
    @ApiResponse(responseCode = "403",
            description = "Rol profesional consultando un bloqueo de otro profesional o de un sillón")
    @ApiResponse(responseCode = "404", description = "El bloqueo no existe")
    public BloqueoRespuesta obtener(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        Bloqueo bloqueo = servicio.obtener(id);
        AccesoPorProfesional.verificarBloqueo(usuario, bloqueo);
        return BloqueoRespuesta.de(bloqueo);
    }

    @SoloAdministrador
    @PostMapping
    @Operation(summary = "Crear un bloqueo",
            description = "Los turnos existentes no se modifican: la respuesta los informa en turnosAfectados")
    @ApiResponse(responseCode = "201", description = "Bloqueo creado")
    @ApiResponse(responseCode = "400",
            description = "Datos faltantes, instantes sin desplazamiento, inicio no anterior al fin o ámbito incoherente")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El profesional o el sillón no existe")
    @ApiResponse(responseCode = "409", description = "El profesional o el sillón está inactivo")
    public ResponseEntity<ResultadoBloqueoRespuesta> crear(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody BloqueoSolicitud solicitud) {
        ResultadoBloqueoRespuesta creado = ResultadoBloqueoRespuesta.de(servicio.crear(solicitud.ambito(),
                solicitud.profesionalId(), solicitud.sillonId(), solicitud.inicio().toInstant(),
                solicitud.fin().toInstant(), solicitud.motivo(), solicitud.detalle(), usuario.id()));
        return ResponseEntity.created(URI.create("/api/bloqueos/" + creado.bloqueo().id())).body(creado);
    }

    @SoloAdministrador
    @PutMapping("/{id}")
    @Operation(summary = "Editar un bloqueo",
            description = "Cambia franja, motivo y detalle; el ámbito y el recurso no se editan")
    @ApiResponse(responseCode = "200", description = "Bloqueo actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o intento de cambiar el ámbito o el recurso")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El bloqueo no existe")
    public ResultadoBloqueoRespuesta editar(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @Valid @RequestBody BloqueoEdicion edicion) {
        return ResultadoBloqueoRespuesta.de(servicio.editar(id, edicion.inicio().toInstant(),
                edicion.fin().toInstant(), edicion.motivo(), edicion.detalle(), edicion.ambito(),
                edicion.profesionalId(), edicion.sillonId(), usuario.id()));
    }

    @SoloAdministrador
    @DeleteMapping("/{id}")
    @Operation(summary = "Borrar un bloqueo")
    @ApiResponse(responseCode = "204", description = "Bloqueo borrado")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador")
    @ApiResponse(responseCode = "404", description = "El bloqueo no existe")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    private static Instant aInstante(OffsetDateTime fecha) {
        return fecha == null ? null : fecha.toInstant();
    }
}

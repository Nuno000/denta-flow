package ar.dentaflow.presentacion.pacientes;

import ar.dentaflow.aplicacion.pacientes.ServicioDePacientes;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.infraestructura.seguridad.guardas.AdministradorORecepcion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Búsqueda de pacientes por DNI para la carga manual de turnos (C-12, design D5): administrador y recepción. Es un
 * {@code POST} porque el DNI es un dato personal (Ley 25.326) y en la URL quedaría en logs de acceso, historial del
 * navegador y proxies. No se escribe el DNI en los logs ni se repite en los errores.
 */
@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "Pacientes", description = "Datos personales de pacientes (Ley 25.326)")
@SecurityRequirement(name = "bearerAuth")
public class PacienteRecepcionController {

    private final ServicioDePacientes pacientes;

    public PacienteRecepcionController(ServicioDePacientes pacientes) {
        this.pacientes = pacientes;
    }

    @AdministradorORecepcion
    @PostMapping("/busqueda")
    @Operation(summary = "Buscar un paciente por DNI",
            description = "Devuelve los datos de contacto del paciente cuyo DNI normalizado coincide. El DNI viaja en "
                    + "el cuerpo, nunca en la URL. La respuesta no se cachea.")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado, con su DNI normalizado")
    @ApiResponse(responseCode = "400", description = "DNI vacío o mal formado")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador ni recepción")
    @ApiResponse(responseCode = "404", description = "No hay un paciente con ese DNI")
    public ResponseEntity<PacienteRespuesta> buscar(@Valid @RequestBody BusquedaDePacienteSolicitud solicitud) {
        PacienteRespuesta paciente = pacientes.buscarPorDni(solicitud.dni())
                .map(PacienteRespuesta::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay un paciente con ese DNI"));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(paciente);
    }
}

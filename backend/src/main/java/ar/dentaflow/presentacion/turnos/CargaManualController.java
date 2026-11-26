package ar.dentaflow.presentacion.turnos;

import ar.dentaflow.aplicacion.turnos.CargaManualDeTurnos;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.AdministradorORecepcion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carga manual de turnos y sobreturnos por recepción y administrador (C-12, US-021 y US-022; spec
 * carga-manual-turnos; design D1). Dos operaciones separadas con la misma guarda: el sobreturno nunca es un flag del
 * cuerpo. El rol se verifica en el backend; el profesional recibe 403.
 */
@RestController
@RequestMapping("/api/turnos")
@Tag(name = "Turnos", description = "Acciones operativas sobre turnos del consultorio")
@SecurityRequirement(name = "bearerAuth")
public class CargaManualController {

    private final CargaManualDeTurnos carga;

    public CargaManualController(CargaManualDeTurnos carga) {
        this.carga = carga;
    }

    @AdministradorORecepcion
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cargar un turno (telefónico o presencial)",
            description = "Registra o actualiza al paciente por DNI y crea un turno pendiente de origen recepción. "
                    + "Rechaza con 409 la superposición de profesional o sillón, el horario de atención y los "
                    + "bloqueos, y en ese caso sugiere hasta 3 horarios libres cercanos del mismo profesional "
                    + "(alternativas). El paciente queda registrado aunque el turno se rechace.")
    @ApiResponse(responseCode = "201", description = "Turno creado, sin token ni datos de contacto del paciente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos: campos del paciente, inicio sin desplazamiento, "
            + "con segundos o en el pasado")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador ni recepción")
    @ApiResponse(responseCode = "404", description = "La prestación, el profesional o el sillón no existe")
    @ApiResponse(responseCode = "409", description = "Horario ocupado, fuera de horario o bloqueado; incluye "
            + "alternativas")
    public TurnoCargadoRespuesta cargar(@Valid @RequestBody SolicitudDeCargaManual solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return TurnoCargadoRespuesta.de(carga.cargar(solicitud.aPedido(), usuario.id()).turno());
    }

    @AdministradorORecepcion
    @PostMapping("/sobreturnos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cargar un sobreturno",
            description = "Igual que la carga común, pero el turno puede superponerse con otros del profesional o del "
                    + "sillón. Sigue respetando horario y bloqueos. Queda auditado con quién y cuándo; no existe en "
                    + "la reserva online.")
    @ApiResponse(responseCode = "201", description = "Sobreturno creado (esSobreturno verdadero)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "El rol no es administrador ni recepción")
    @ApiResponse(responseCode = "404", description = "La prestación, el profesional o el sillón no existe")
    @ApiResponse(responseCode = "409", description = "Fuera del horario de atención o dentro de un bloqueo; sin "
            + "alternativas")
    public TurnoCargadoRespuesta cargarSobreturno(@Valid @RequestBody SolicitudDeCargaManual solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return TurnoCargadoRespuesta.de(carga.cargarSobreturno(solicitud.aPedido(), usuario.id()).turno());
    }
}

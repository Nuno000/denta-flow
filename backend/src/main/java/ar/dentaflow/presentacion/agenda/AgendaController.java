package ar.dentaflow.presentacion.agenda;

import ar.dentaflow.aplicacion.agenda.ConsultaDeAgenda;
import ar.dentaflow.aplicacion.agenda.PedidoDeAgenda;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.PersonalInterno;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agenda interna (C-11; US-020, US-023; spec agenda-interna). Solo lectura: los tres roles internos, con la regla
 * de "propios" para el profesional, que siempre ve únicamente sus turnos (design D2).
 */
@RestController
@RequestMapping("/api/agenda")
@Tag(name = "Agenda", description = "Turnos del consultorio por día o semana (solo lectura)")
@SecurityRequirement(name = "bearerAuth")
public class AgendaController {

    private final ConsultaDeAgenda consulta;

    public AgendaController(ConsultaDeAgenda consulta) {
        this.consulta = consulta;
    }

    @PersonalInterno
    @GetMapping("/turnos")
    @Operation(summary = "Turnos de la agenda en un rango de días del consultorio",
            description = "Devuelve los turnos cuyo horario se cruza con los días pedidos (America/Argentina/"
                    + "Buenos_Aires, máximo 7 días), ordenados por inicio, profesional y sillón. Sin estado, excluye "
                    + "los cancelados. El rol profesional solo ve sus propios turnos.")
    @ApiResponse(responseCode = "200", description = "Turnos del rango, sin datos sensibles del paciente")
    @ApiResponse(responseCode = "400", description = "Fecha mal formada, rango inválido o estado desconocido")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "Un profesional pide la agenda de otro o no está vinculado")
    public List<TurnoDeAgendaRespuesta> turnos(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Primer día, AAAA-MM-DD (hora del consultorio)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Último día, inclusivo")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long profesionalId,
            @RequestParam(required = false) Long sillonId,
            @Parameter(description = "pendiente, confirmado, asistio, ausente, ausente_con_aviso o cancelado")
            @RequestParam(required = false) String estado) {
        PedidoDeAgenda pedido = new PedidoDeAgenda(desde, hasta, profesionalId, sillonId, estado);
        return consulta.consultar(pedido, usuario.rol(), usuario.profesionalId()).stream()
                .map(TurnoDeAgendaRespuesta::de)
                .toList();
    }
}

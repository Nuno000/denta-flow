package ar.dentaflow.presentacion.reportes;

import ar.dentaflow.aplicacion.reportes.ConsultaDeAusentismo;
import ar.dentaflow.aplicacion.reportes.PedidoDeAusentismo;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.PersonalInterno;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Panel de ausentismo (C-23, US-031; spec panel-ausentismo). Solo lectura: administrador y recepción consultan todo
 * el consultorio o filtran por profesional; el rol profesional ve únicamente sus propios indicadores.
 */
@RestController
@RequestMapping("/api/reportes")
@Tag(name = "Reportes", description = "Indicadores de ausentismo del consultorio (solo lectura)")
@SecurityRequirement(name = "bearerAuth")
public class AusentismoController {

    private final ConsultaDeAusentismo consulta;

    public AusentismoController(ConsultaDeAusentismo consulta) {
        this.consulta = consulta;
    }

    @PersonalInterno
    @GetMapping("/ausentismo")
    @Operation(summary = "Indicadores de ausentismo en un rango de fechas",
            description = "Calcula la tasa de ausentismo general y desglosada por profesional, franja horaria, "
                    + "día de la semana y pacientes con ausencias reiteradas a partir de los turnos con asistencia "
                    + "registrada (asistio, ausente, ausente_con_aviso) en America/Argentina/Buenos_Aires.")
    @ApiResponse(responseCode = "200", description = "Indicadores calculados sin datos sensibles del paciente")
    @ApiResponse(responseCode = "400", description = "Fechas inválidas, rango mayor a 366 días o mínimo menor que 1")
    @ApiResponse(responseCode = "401", description = "Sin sesión")
    @ApiResponse(responseCode = "403", description = "Profesional consultando otro profesional o sin vínculo")
    public ReporteDeAusentismoRespuesta ausentismo(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Parameter(description = "Primer día, AAAA-MM-DD (hora del consultorio)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Último día, inclusivo, AAAA-MM-DD")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Filtrar por profesional (ignorado/restringido para rol profesional)")
            @RequestParam(required = false) Long profesionalId,
            @Parameter(description = "Mínimo de ausencias para listar pacientes reiterados (por defecto 2)")
            @RequestParam(required = false) Integer minimoAusencias) {
        PedidoDeAusentismo pedido = new PedidoDeAusentismo(desde, hasta, profesionalId, minimoAusencias);
        return ReporteDeAusentismoRespuesta.de(
                consulta.consultar(pedido, usuario.rol(), usuario.profesionalId()));
    }
}

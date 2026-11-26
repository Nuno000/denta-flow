package ar.dentaflow.presentacion.listaespera;

import ar.dentaflow.aplicacion.listaespera.ServicioDeListaDeEspera;
import ar.dentaflow.dominio.listaespera.EstadoListaEspera;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import ar.dentaflow.infraestructura.seguridad.guardas.AdministradorORecepcion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lista-espera")
@Tag(name = "Lista de espera", description = "Gestión interna de la lista de espera (recepción y administrador)")
@SecurityRequirement(name = "bearerAuth")
public class ListaEsperaController {

    private final ServicioDeListaDeEspera servicio;

    public ListaEsperaController(ServicioDeListaDeEspera servicio) {
        this.servicio = servicio;
    }

    @AdministradorORecepcion
    @GetMapping
    @Operation(summary = "Listar entradas de lista de espera")
    public List<ListaEsperaRespuesta> listar(@RequestParam(required = false) String estado) {
        EstadoListaEspera estadoEnum = (estado == null || estado.isBlank())
                ? null
                : EstadoListaEspera.desdeCodigo(estado.strip());
        return servicio.listar(estadoEnum).stream().map(ListaEsperaRespuesta::de).toList();
    }

    @AdministradorORecepcion
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar paciente en la lista de espera")
    public ListaEsperaRespuesta registrar(@Valid @RequestBody SolicitudCrearListaEspera solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ListaEsperaRespuesta.de(servicio.registrar(solicitud.aSolicitud(), usuario.id()));
    }

    @AdministradorORecepcion
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Dar de baja una entrada activa de la lista de espera")
    public void darDeBaja(@PathVariable long id) {
        servicio.darDeBaja(id);
    }
}

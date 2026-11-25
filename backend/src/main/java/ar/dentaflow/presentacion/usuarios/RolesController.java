package ar.dentaflow.presentacion.usuarios;

import ar.dentaflow.aplicacion.usuarios.ServicioDeUsuarios;
import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de roles (spec gestion-usuarios, "Catálogo de roles"). */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Usuarios", description = "Gestión de usuarios y roles por el administrador")
@SecurityRequirement(name = "bearerAuth")
public class RolesController {

    private final ServicioDeUsuarios servicio;

    public RolesController(ServicioDeUsuarios servicio) {
        this.servicio = servicio;
    }

    @SoloAdministrador
    @GetMapping
    @Operation(summary = "Listar los roles con su nombre visible")
    public List<RolRespuesta> listar() {
        return servicio.listarRoles().stream().map(RolRespuesta::de).toList();
    }
}

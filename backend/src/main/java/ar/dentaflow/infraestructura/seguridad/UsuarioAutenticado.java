package ar.dentaflow.infraestructura.seguridad;

import ar.dentaflow.dominio.usuarios.Rol;

/**
 * El usuario que hizo el request, con el rol y el estado vigentes al momento del request (design D3).
 * Es el principal de la autenticación; los controladores lo reciben con {@code @AuthenticationPrincipal}.
 */
public record UsuarioAutenticado(Long id, String email, Rol rol, Long profesionalId) {
}

package ar.dentaflow.infraestructura.seguridad;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quién está operando en el request actual. Pensado para la auditoría de C-08 (RN-AU-01): así el
 * resto del código no depende de Spring Security para saber el actor.
 */
@Component
public class UsuarioActual {

    public Optional<UsuarioAutenticado> obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return Optional.of(usuario);
        }
        return Optional.empty();
    }
}

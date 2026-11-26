package ar.dentaflow.presentacion.disponibilidad;

import ar.dentaflow.dominio.disponibilidad.Bloqueo;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.infraestructura.seguridad.UsuarioAutenticado;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;

/**
 * Regla "propios" de la matriz RBAC para horarios y bloqueos (design D8): administrador y recepción
 * leen todo; un usuario con rol profesional solo lo de su profesional y, en bloqueos, también los del
 * consultorio. Un profesional sin vínculo no ve nada. Se rechaza con 403.
 */
final class AccesoPorProfesional {

    private AccesoPorProfesional() {
    }

    /** Falla con 403 si el usuario tiene rol profesional y el profesional pedido no es el suyo. */
    static void verificarProfesional(UsuarioAutenticado usuario, Long profesionalId) {
        profesionalPropio(usuario).ifPresent(propio -> {
            if (!propio.equals(profesionalId)) {
                throw sinPermiso();
            }
        });
    }

    /** El profesional del usuario si tiene rol profesional; vacío para los demás roles. */
    static Optional<Long> profesionalPropio(UsuarioAutenticado usuario) {
        if (usuario.rol() != Rol.PROFESIONAL) {
            return Optional.empty();
        }
        if (usuario.profesionalId() == null) {
            throw sinPermiso();
        }
        return Optional.of(usuario.profesionalId());
    }

    static boolean puedeVerBloqueo(UsuarioAutenticado usuario, Bloqueo bloqueo) {
        if (usuario.rol() != Rol.PROFESIONAL) {
            return true;
        }
        return usuario.profesionalId() != null
                && bloqueo.afectaAProfesional(Objects.requireNonNull(usuario.profesionalId()));
    }

    /** Falla con 403 si el usuario tiene rol profesional y el bloqueo no es suyo ni del consultorio. */
    static void verificarBloqueo(UsuarioAutenticado usuario, Bloqueo bloqueo) {
        if (!puedeVerBloqueo(usuario, bloqueo)) {
            throw new AccessDeniedException("Solo podés ver los bloqueos de tu profesional y los del consultorio");
        }
    }

    private static AccessDeniedException sinPermiso() {
        return new AccessDeniedException("Solo podés ver los datos de tu propio profesional");
    }
}

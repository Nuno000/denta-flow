package ar.dentaflow.infraestructura.seguridad.guardas;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Guarda de rol (C-04, design D8): Solo el rol administrador. Se verifica en el backend; un rol no
 * permitido recibe 403. Cada endpoint protegido declara una guarda según la matriz de
 * {@code 03_actores_y_roles.md}.
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMINISTRADOR')")
public @interface SoloAdministrador {
}

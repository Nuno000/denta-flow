package ar.dentaflow.infraestructura.seguridad;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentación OpenAPI con el esquema {@code bearerAuth} (design D8): en Swagger UI se ingresa con
 * {@code POST /api/auth/login}, se copia el token en "Authorize" y se prueban los endpoints protegidos.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(info = @Info(title = "DentaFlow API", version = "v1",
        description = "Turnos online para consultorios odontológicos"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
        description = "Token de POST /api/auth/login")
public class ConfiguracionOpenApi {
}

package ar.dentaflow.infraestructura.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Respuestas 401 y 403 del filtro de seguridad, antes de llegar a un controlador, con el mismo formato
 * {@code application/problem+json} que {@code ManejadorDeErrores} (spec "Errores de acceso en JSON").
 */
@Component
public class RespuestasDeSeguridad implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        escribir(response, HttpServletResponse.SC_UNAUTHORIZED, "Ingreso requerido",
                "Tenés que ingresar para usar esta operación");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        escribir(response, HttpServletResponse.SC_FORBIDDEN, "Sin permiso",
                "No tenés permiso para realizar esta operación");
    }

    private static void escribir(HttpServletResponse response, int estado, String titulo, String detalle)
            throws IOException {
        response.setStatus(estado);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + titulo + "\",\"status\":" + estado
                + ",\"detail\":\"" + detalle + "\"}");
    }
}

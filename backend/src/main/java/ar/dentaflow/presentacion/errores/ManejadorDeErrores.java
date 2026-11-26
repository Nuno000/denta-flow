package ar.dentaflow.presentacion.errores;

import ar.dentaflow.aplicacion.reservas.DemasiadasReservasException;
import ar.dentaflow.aplicacion.usuarios.CredencialesInvalidasException;
import ar.dentaflow.aplicacion.usuarios.DemasiadosIntentosException;
import ar.dentaflow.dominio.comun.ConflictoException;
import ar.dentaflow.dominio.comun.OperacionNoPermitidaException;
import ar.dentaflow.dominio.comun.RecursoNoEncontradoException;
import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce las excepciones a {@link ProblemDetail} ({@code application/problem+json}, RFC 9457) con
 * título y detalle en español (design D9). Hereda el manejo estándar de Spring MVC (404 de ruta, 405,
 * 415...) y nunca devuelve trazas ni mensajes internos. Lo reutilizan todos los changes.
 */
@RestControllerAdvice
public class ManejadorDeErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorDeErrores.class);

    public static final String TITULO_DATOS_INVALIDOS = "Datos inválidos";
    public static final String DETALLE_SIN_PERMISO = "No tenés permiso para realizar esta operación";

    static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }

    private static ResponseEntity<ProblemDetail> responder(HttpStatus estado, String titulo, String detalle) {
        return ResponseEntity.status(estado).body(problema(estado, titulo, detalle));
    }

    @ExceptionHandler(ValidacionDeDominioException.class)
    ResponseEntity<ProblemDetail> validacion(ValidacionDeDominioException e) {
        return responder(HttpStatus.BAD_REQUEST, TITULO_DATOS_INVALIDOS, e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<ProblemDetail> noEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage());
    }

    @ExceptionHandler({ConflictoException.class, OperacionNoPermitidaException.class})
    ResponseEntity<ProblemDetail> conflicto(RuntimeException e) {
        return responder(HttpStatus.CONFLICT, "Conflicto", e.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<ProblemDetail> credenciales(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", e.getMessage());
    }

    @ExceptionHandler(DemasiadosIntentosException.class)
    ResponseEntity<ProblemDetail> demasiadosIntentos(DemasiadosIntentosException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(e.reintentarEn().toSeconds()))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos", e.getMessage()));
    }

    /** C-16 design D5: límite de reservas públicas por origen. */
    @ExceptionHandler(DemasiadasReservasException.class)
    ResponseEntity<ProblemDetail> demasiadasReservas(DemasiadasReservasException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(e.reintentarEn().toSeconds()))
                .body(problema(HttpStatus.TOO_MANY_REQUESTS, "Demasiados pedidos", e.getMessage()));
    }

    /** Lo lanza la seguridad por método ({@code @PreAuthorize}) dentro del controlador. */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> sinPermiso(AccessDeniedException e) {
        return responder(HttpStatus.FORBIDDEN, "Sin permiso", DETALLE_SIN_PERMISO);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ProblemDetail> sinAutenticar(AuthenticationException e) {
        return responder(HttpStatus.UNAUTHORIZED, "Ingreso requerido", "Tenés que ingresar para usar esta operación");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> inesperado(Exception e) {
        LOG.error("Error inesperado al atender el pedido ({})", e.getClass().getSimpleName(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Probá de nuevo en unos minutos.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, TITULO_DATOS_INVALIDOS,
                "Revisá los datos marcados");
        problema.setProperty("errores", errores);
        return ResponseEntity.badRequest().body(problema);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest().body(problema(HttpStatus.BAD_REQUEST, TITULO_DATOS_INVALIDOS,
                "El cuerpo del pedido no es un JSON válido o tiene datos de tipo incorrecto"));
    }
}

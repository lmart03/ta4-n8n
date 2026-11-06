package pe.edu.ta4.shared.error;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Único punto de salida de errores de la API: todo se responde como Problem Details (RFC 9457,
 * {@code application/problem+json}) con {@code type}, {@code title}, {@code status}, {@code detail},
 * {@code instance} y un {@code traceId} que también queda en el log.
 *
 * <p>Extiende {@link ResponseEntityExceptionHandler}, que ya traduce los errores de Spring MVC
 * (parámetro faltante, método no permitido, tipo de contenido no soportado...) a {@link ProblemDetail}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> handleApi(ApiException error, WebRequest request) {
        ProblemType type = error.type();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(type.status()), error.getMessage());
        problem.setType(type.uri());
        problem.setTitle(type.title());
        if (!error.issues().isEmpty()) problem.setProperty("errors", error.issues());
        return handleExceptionInternal(error, problem, new HttpHeaders(), HttpStatusCode.valueOf(type.status()), request);
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Object> handleDatabase(DataAccessException error, WebRequest request) {
        return unexpected(error, ProblemType.SERVICE_UNAVAILABLE, "Los datos no están disponibles temporalmente.", request);
    }

    /** Red de seguridad: cualquier otra excepción es un 500 con detalle genérico (nunca filtra datos internos). */
    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception error, WebRequest request) {
        if (error instanceof MaxUploadSizeExceededException) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(413), "El archivo supera el tamaño máximo permitido.");
            problem.setType(ProblemType.PAYLOAD_TOO_LARGE.uri());
            problem.setTitle(ProblemType.PAYLOAD_TOO_LARGE.title());
            return handleExceptionInternal(error, problem, new HttpHeaders(), HttpStatusCode.valueOf(413), request);
        }
        return unexpected(error, ProblemType.INTERNAL_ERROR, "Ocurrió un error inesperado.", request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ProblemIssue> issues = new ArrayList<>();
        for (FieldError field : error.getBindingResult().getFieldErrors()) {
            issues.add(ProblemIssue.of("#/" + field.getField(), "INVALID_FIELD", message(field.getDefaultMessage())));
        }
        error.getBindingResult().getGlobalErrors().forEach(global ->
            issues.add(ProblemIssue.of("INVALID_REQUEST", message(global.getDefaultMessage()))));
        ProblemDetail body = error.getBody();
        body.setProperty("errors", issues);
        return handleExceptionInternal(error, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException error,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ProblemIssue> issues = new ArrayList<>();
        error.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(item ->
                issues.add(ProblemIssue.of("#/" + name, "INVALID_PARAMETER", message(item.getDefaultMessage()))));
        });
        ProblemDetail body = error.getBody();
        body.setProperty("errors", issues);
        return handleExceptionInternal(error, body, headers, status, request);
    }

    /** Punto común: completa el {@code type} y agrega el {@code traceId} a todo Problem Details. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, @Nullable Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        // Primero lo arma Spring (para los errores del framework el ProblemDetail se crea aquí dentro) y
        // después se completa, así el type y el traceId salen igual en TODOS los errores.
        ResponseEntity<Object> response = super.handleExceptionInternal(error, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            if (problem.getType() == null || "about:blank".equals(problem.getType().toString())) {
                ProblemType.forStatus(statusCode.value()).ifPresent(type -> problem.setType(type.uri()));
            }
            if (problem.getProperties() == null || !problem.getProperties().containsKey("traceId")) {
                problem.setProperty("traceId", UUID.randomUUID().toString());
            }
            if (statusCode.is4xxClientError()) {
                log.info("[{}] {} {}: {}", problem.getProperties().get("traceId"), statusCode.value(),
                    error.getClass().getSimpleName(), error.getMessage());
            }
        }
        return response;
    }

    private ResponseEntity<Object> unexpected(Exception error, ProblemType type, String detail, WebRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("[{}] {}", traceId, detail, error);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(type.status()), detail);
        problem.setType(type.uri());
        problem.setTitle(type.title());
        problem.setProperty("traceId", traceId);
        return handleExceptionInternal(error, problem, new HttpHeaders(), HttpStatusCode.valueOf(type.status()), request);
    }

    private static String message(@Nullable String value) {
        return value == null ? "Valor inválido" : value;
    }
}

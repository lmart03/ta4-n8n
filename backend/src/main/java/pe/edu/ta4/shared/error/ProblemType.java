package pe.edu.ta4.shared.error;

import java.net.URI;
import java.util.Arrays;
import java.util.Optional;

/**
 * Catálogo de tipos de problema de la API (RFC 9457). Cada tipo tiene una URI estable
 * ({@code /problems/{slug}}) que se puede abrir para leer su descripción.
 */
public enum ProblemType {
    VALIDATION_ERROR("validation-error", 400, "Solicitud inválida",
        "La solicitud está mal formada o le faltan parámetros obligatorios."),
    UNAUTHORIZED("unauthorized", 401, "No autorizado",
        "Falta la clave de acceso o no es válida."),
    NOT_FOUND("not-found", 404, "Recurso no encontrado",
        "El recurso solicitado no existe."),
    METHOD_NOT_ALLOWED("method-not-allowed", 405, "Método no permitido",
        "La ruta existe pero no admite ese método HTTP."),
    CONFLICT("conflict", 409, "Conflicto de estado",
        "La operación no es válida para el estado actual del recurso, por ejemplo entregar dos veces el resultado de un trabajo."),
    TOO_MANY_REQUESTS("too-many-requests", 429, "Demasiados intentos",
        "Se hicieron demasiados intentos fallidos; espera un minuto antes de reintentar."),
    PAYLOAD_TOO_LARGE("payload-too-large", 413, "Archivo demasiado grande",
        "El archivo enviado supera el tamaño máximo permitido."),
    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", 415, "Tipo de contenido no soportado",
        "La API no acepta el tipo de contenido enviado."),
    INVALID_WORKBOOK("ruc-invalid-workbook", 422, "Excel de RUC inválido",
        "El Excel se pudo leer, pero contiene filas inválidas: RUC mal formado, dígito verificador incorrecto, duplicados o cantidad fuera de rango."),
    INVALID_RESULT_FILE("ruc-invalid-result-file", 422, "Archivo de resultados inválido",
        "El ZIP de resultados no es válido: no es un ZIP, es inseguro, no contiene el TXT de SUNAT o no tiene las columnas esperadas."),
    BAD_GATEWAY("bad-gateway", 502, "Servicio externo con error",
        "Un servicio externo, como Telegram, rechazó la operación o no respondió."),
    SERVICE_UNAVAILABLE("service-unavailable", 503, "Servicio no disponible",
        "Un recurso necesario, como la base de datos o el almacenamiento de archivos, no está disponible temporalmente."),
    INTERNAL_ERROR("internal-error", 500, "Error interno",
        "Ocurrió un error inesperado. Cita el traceId al reportarlo.");

    private final String slug;
    private final int status;
    private final String title;
    private final String description;

    ProblemType(String slug, int status, String title, String description) {
        this.slug = slug;
        this.status = status;
        this.title = title;
        this.description = description;
    }

    public String slug() { return slug; }
    public int status() { return status; }
    public String title() { return title; }
    public String description() { return description; }

    /** URI del tipo, relativa a la API: se resuelve contra la URL de la petición (RFC 9457 §3.1.1). */
    public URI uri() { return URI.create("/problems/" + slug); }

    public static Optional<ProblemType> bySlug(String slug) {
        return Arrays.stream(values()).filter(type -> type.slug.equals(slug)).findFirst();
    }

    /** Tipo genérico para un código de estado, para errores que lanza el propio framework (404, 405, 415...). */
    public static Optional<ProblemType> forStatus(int status) {
        return switch (status) {
            case 400 -> Optional.of(VALIDATION_ERROR);
            case 401 -> Optional.of(UNAUTHORIZED);
            case 404 -> Optional.of(NOT_FOUND);
            case 405 -> Optional.of(METHOD_NOT_ALLOWED);
            case 409 -> Optional.of(CONFLICT);
            case 429 -> Optional.of(TOO_MANY_REQUESTS);
            case 502 -> Optional.of(BAD_GATEWAY);
            case 413 -> Optional.of(PAYLOAD_TOO_LARGE);
            case 415 -> Optional.of(UNSUPPORTED_MEDIA_TYPE);
            case 503 -> Optional.of(SERVICE_UNAVAILABLE);
            case 500 -> Optional.of(INTERNAL_ERROR);
            default -> Optional.empty();
        };
    }
}

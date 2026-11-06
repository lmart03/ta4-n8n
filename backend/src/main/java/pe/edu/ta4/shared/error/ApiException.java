package pe.edu.ta4.shared.error;

import java.util.List;

/**
 * Excepción base de la API. El {@link GlobalExceptionHandler} la convierte en una respuesta
 * {@code application/problem+json} (RFC 9457) según su {@link ProblemType}.
 */
public class ApiException extends RuntimeException {
    private final ProblemType type;
    private final transient List<ProblemIssue> issues;

    public ApiException(ProblemType type, String detail) {
        this(type, detail, List.of());
    }

    public ApiException(ProblemType type, String detail, List<ProblemIssue> issues) {
        super(detail);
        this.type = type;
        this.issues = List.copyOf(issues);
    }

    public ProblemType type() { return type; }
    public List<ProblemIssue> issues() { return issues; }

    public static ApiException notFound(String detail) {
        return new ApiException(ProblemType.NOT_FOUND, detail);
    }

    public static ApiException conflict(String detail) {
        return new ApiException(ProblemType.CONFLICT, detail);
    }

    public static ApiException badRequest(String detail) {
        return new ApiException(ProblemType.VALIDATION_ERROR, detail);
    }

    public static ApiException unauthorized(String detail) {
        return new ApiException(ProblemType.UNAUTHORIZED, detail);
    }

    public static ApiException tooManyRequests(String detail) {
        return new ApiException(ProblemType.TOO_MANY_REQUESTS, detail);
    }

    public static ApiException badGateway(String detail) {
        return new ApiException(ProblemType.BAD_GATEWAY, detail);
    }

    public static ApiException unavailable(String detail) {
        return new ApiException(ProblemType.SERVICE_UNAVAILABLE, detail);
    }

    public static ApiException invalidWorkbook(String detail, List<ProblemIssue> issues) {
        return new ApiException(ProblemType.INVALID_WORKBOOK, detail, issues);
    }

    public static ApiException invalidResultFile(String detail, List<ProblemIssue> issues) {
        return new ApiException(ProblemType.INVALID_RESULT_FILE, detail, issues);
    }
}

package pe.edu.ta4.shared.error;

/**
 * Un problema puntual dentro de {@code errors[]} (extensión sobre RFC 9457).
 *
 * @param pointer ubicación como JSON Pointer (RFC 6901), por ejemplo {@code #/rows/4/ruc}; puede ser nulo
 * @param code    código estable para que el cliente decida qué hacer
 * @param detail  explicación legible
 */
public record ProblemIssue(String pointer, String code, String detail) {
    public static ProblemIssue of(String pointer, String code, String detail) {
        return new ProblemIssue(pointer, code, detail);
    }

    public static ProblemIssue of(String code, String detail) {
        return new ProblemIssue(null, code, detail);
    }
}

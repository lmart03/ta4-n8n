/** Un problema puntual dentro de `errors[]` (extensión propia de la API sobre RFC 9457). */
export interface ProblemIssue {
  /** Ubicación del problema como JSON Pointer (RFC 6901), p. ej. `#/rows/4/ruc`. */
  pointer?: string;
  code?: string;
  detail: string;
}

/** Cuerpo de error estándar de la API: Problem Details (RFC 9457), `application/problem+json`. */
export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  traceId?: string;
  errors?: ProblemIssue[];
}

/** Reconoce un cuerpo Problem Details sin asumir nada sobre su origen. */
export function isProblemDetails(value: unknown): value is ProblemDetails {
  if (typeof value !== 'object' || value === null) return false;
  const candidate = value as Record<string, unknown>;
  return typeof candidate['title'] === 'string' || typeof candidate['detail'] === 'string';
}

import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetails, isProblemDetails } from 'src/app/core/infrastructure/api/models/problem-details.model';

/** Cada cuánto se consulta el avance mientras el flujo está en marcha. */
export const POLL_INTERVAL_MS = 1500;
/** Cada cuánto se pregunta si el robot está listo. */
export const ROBOT_POLL_INTERVAL_MS = 3000;

const NETWORK_ERROR = 'No se pudo contactar con el servidor. Verifica que el backend esté en marcha.';

/** Convierte cualquier fallo HTTP en un Problem Details mostrable. */
export function toProblem(error: unknown): ProblemDetails {
  if (error instanceof HttpErrorResponse) {
    if (isProblemDetails(error.error)) return error.error;
    if (error.status === 0) return { title: NETWORK_ERROR, status: 0 };
    return { title: 'No se pudo completar la operación.', status: error.status };
  }
  return { title: 'Ocurrió un error inesperado.' };
}

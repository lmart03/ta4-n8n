import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetails, isProblemDetails } from 'src/app/core/infrastructure/api/models/problem-details.model';

/** Cada cuánto se revisa el estado de las piezas mientras la pantalla de estado está abierta. */
export const STATUS_POLL_INTERVAL_MS = 5000;

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

/** Texto corto para mostrar junto a un botón: el detalle del backend si existe, si no el título. */
export function problemText(problem: ProblemDetails): string {
  return problem.detail ?? problem.title ?? 'No se pudo completar la operación.';
}

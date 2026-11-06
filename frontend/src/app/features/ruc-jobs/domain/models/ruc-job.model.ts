/** Estados de un trabajo de consulta de RUC (los mismos que devuelve el backend). */
export type RucJobStatus =
  | 'RECIBIDO'
  | 'VALIDANDO'
  | 'LISTO_PARA_ROBOT'
  | 'EN_CONSULTA'
  | 'REQUIERE_INTERVENCION'
  | 'PROCESANDO_RESULTADO'
  | 'COMPLETADO'
  | 'COMPLETADO_CON_OBSERVACIONES'
  | 'FALLIDO';

/** Trabajo creado al cargar el Excel de RUC. */
export interface RucJob {
  id: string;
  status: RucJobStatus;
  /** Cantidad de RUC aceptados en el trabajo. */
  total: number;
  foundCount: number;
  missingCount: number;
  unexpectedCount: number;
  originalFilename: string;
  /** Resumen con observaciones cuando el trabajo termina. */
  summary: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  /** Cuándo se pidió arrancar el robot; nulo si no hay un arranque pendiente. */
  startRequestedAt: string | null;
}

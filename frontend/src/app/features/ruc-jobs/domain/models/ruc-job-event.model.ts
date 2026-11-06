import { RucJobStatus } from './ruc-job.model';

/** Una entrada del historial del trabajo: cambios de estado y rechazos. */
export interface RucJobEvent {
  sequence: number;
  occurredAt: string;
  from: RucJobStatus | null;
  to: RucJobStatus;
  message: string | null;
}

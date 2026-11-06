import { RucJob } from '../models/ruc-job.model';

/**
 * El trabajo sigue en marcha y conviene seguir consultándolo: solo terminó bien, o falló sin un reintento pendiente,
 * detiene la consulta.
 */
export function isJobActive(job: RucJob): boolean {
  if (job.status === 'COMPLETADO' || job.status === 'COMPLETADO_CON_OBSERVACIONES') return false;
  if (job.status === 'FALLIDO') return job.startRequestedAt !== null;
  return true;
}

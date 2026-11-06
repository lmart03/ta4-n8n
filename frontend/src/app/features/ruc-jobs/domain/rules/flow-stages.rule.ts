import { FlowPhase, FlowStage, FlowStageKey, FlowStageState, FlowView } from '../models/flow.model';
import { RucJobEvent } from '../models/ruc-job-event.model';
import { RucJob, RucJobStatus } from '../models/ruc-job.model';

/** Pasado este tiempo esperando al robot sin que arranque, se avisa que el lanzador local puede no estar abierto. */
export const LAUNCHER_WAIT_HINT_MS = 8000;

/** Qué tan avanzado está cada estado del backend (FALLIDO no tiene avance propio: se deduce del historial). */
const RANK: Record<Exclude<RucJobStatus, 'FALLIDO'>, number> = {
  RECIBIDO: 0,
  VALIDANDO: 1,
  LISTO_PARA_ROBOT: 2,
  EN_CONSULTA: 3,
  REQUIERE_INTERVENCION: 3,
  PROCESANDO_RESULTADO: 4,
  COMPLETADO: 5,
  COMPLETADO_CON_OBSERVACIONES: 5,
};

const STAGE_COPY: Record<FlowStageKey, { title: string; description: string }> = {
  excel: { title: 'Excel validado', description: 'Revisamos cada RUC: formato, dígito verificador y duplicados.' },
  txt: { title: 'Archivo para SUNAT', description: 'Generamos el TXT que pide la Consulta Múltiple.' },
  sunat: { title: 'Consulta en SUNAT', description: 'El robot sube el archivo y descarga el ZIP de resultados.' },
  zip: { title: 'Resultados leídos', description: 'Abrimos el ZIP y cotejamos cada RUC pedido.' },
  results: { title: 'Datos listos', description: 'Razón social, dirección, condición y fecha de inicio.' },
};

const ORDER: FlowStageKey[] = ['excel', 'txt', 'sunat', 'zip', 'results'];

function rankOf(status: RucJobStatus, events: RucJobEvent[]): number {
  if (status !== 'FALLIDO') return RANK[status];
  // Un trabajo fallido se queda en la etapa donde estaba: se deduce por lo más lejos que llegó.
  const reached = events.filter((event) => event.to !== 'FALLIDO').map((event) => RANK[event.to as keyof typeof RANK]);
  return Math.max(2, ...reached);
}

/**
 * El arranque ya se pidió (y quizá el lanzador ya lo tomó, lo que borra `startRequestedAt`). Pedirlo y tomarlo
 * dejan en el historial un evento que no cambia de estado (de LISTO_PARA_ROBOT a LISTO_PARA_ROBOT).
 */
function startWasAsked(events: RucJobEvent[]): boolean {
  return events.some((event) => event.from === 'LISTO_PARA_ROBOT' && event.to === 'LISTO_PARA_ROBOT');
}

function stageState(key: FlowStageKey, job: RucJob, rank: number, startAsked: boolean): FlowStageState {
  const failed = job.status === 'FALLIDO';
  switch (key) {
    case 'excel':
      return rank >= 2 ? 'done' : 'active';
    case 'txt':
      return rank >= 2 ? 'done' : 'pending';
    case 'sunat':
      if (rank >= 4) return 'done';
      if (failed) return 'error';
      if (rank === 3) return job.status === 'REQUIERE_INTERVENCION' ? 'attention' : 'active';
      return rank === 2 && (job.startRequestedAt || startAsked) ? 'active' : 'pending';
    case 'zip':
      if (rank >= 5) return 'done';
      if (failed && rank === 4) return 'error';
      return rank === 4 ? 'active' : 'pending';
    case 'results':
      return rank >= 5 ? 'done' : 'pending';
  }
}

function stageDetail(key: FlowStageKey, state: FlowStageState, job: RucJob): string | null {
  switch (key) {
    case 'excel':
      return state === 'done' ? `${job.total} RUC válidos` : null;
    case 'txt':
      return state === 'done' ? 'Listo para subir' : null;
    case 'sunat':
      if (state === 'error') return 'Se detuvo en esta etapa.';
      if (state === 'attention') return 'SUNAT pide una verificación: resuélvela en Edge y el robot sigue solo.';
      if (state === 'active') {
        if (job.status !== 'LISTO_PARA_ROBOT') return 'Subiendo el archivo y descargando el ZIP…';
        // Pedido en cola (nadie lo tomó aún) o ya tomado por el lanzador (el robot está abriendo UiPath).
        return job.startRequestedAt ? 'Esperando que el lanzador tome el pedido…' : 'El robot está arrancando…';
      }
      return null;
    case 'zip':
      if (state === 'error') return 'Se detuvo en esta etapa.';
      return state === 'active' ? 'Cotejando cada RUC…' : null;
    case 'results':
      return state === 'done' ? job.summary : null;
  }
}

function headlineFor(phase: FlowPhase, job: RucJob | null, rank: number, viaApi = false): string {
  switch (phase) {
    case 'idle':
      return 'Adjunta tu Excel de RUC';
    case 'ready':
      return 'Todo listo para empezar';
    case 'attention':
      return 'Falta tu ayuda en SUNAT';
    case 'failed':
      return 'El flujo se detuvo';
    case 'done':
      if (viaApi) return `Listo por API de respaldo: ${job?.foundCount ?? 0} de ${job?.total ?? 0} RUC con datos`;
      return job?.status === 'COMPLETADO'
        ? `Listo: ${job.foundCount} de ${job.total} RUC con datos de SUNAT`
        : `Terminó con observaciones: ${job?.foundCount ?? 0} de ${job?.total ?? 0} RUC con datos`;
    case 'running':
      if (rank < 2) return 'Validando tu Excel…';
      if (rank === 2) return 'Esperando al robot…';
      if (viaApi) return 'El robot no pudo; consultando por la API de respaldo…';
      return rank === 3 ? 'El robot está consultando en SUNAT…' : 'Procesando los resultados…';
  }
}

/**
 * Traduce el estado del trabajo (y su historial) a lo que ve el usuario: las 5 etapas con su estado, el titular,
 * y un aviso cuando una espera es anormal. Es una función pura: no conoce Angular ni HTTP.
 *
 * @param nowMs hora actual en milisegundos (se inyecta para poder probarlo)
 */
export function deriveFlow(job: RucJob | null, events: RucJobEvent[], nowMs: number): FlowView {
  const latest = events.length > 0 ? (events[events.length - 1].message ?? null) : null;

  if (!job) {
    const stages: FlowStage[] = ORDER.map((key) => ({
      key,
      ...STAGE_COPY[key],
      state: 'pending',
      detail: null,
      robotWorking: false,
    }));
    return { phase: 'idle', headline: headlineFor('idle', null, 0), stages, hint: null, latestMessage: null };
  }

  const rank = rankOf(job.status, events);
  const startAsked = startWasAsked(events);
  const viaApi = usedApi(events);
  const stages: FlowStage[] = ORDER.map((key) => {
    const state = stageState(key, job, rank, startAsked);
    return {
      key,
      ...STAGE_COPY[key],
      state,
      detail: viaApi ? apiDetail(key, state, job) : stageDetail(key, state, job),
      // Solo trabaja de verdad cuando el robot ya reportó que está en la consulta.
      robotWorking: key === 'sunat' && state === 'active' && job.status === 'EN_CONSULTA',
    };
  });

  let phase: FlowPhase;
  if (job.status === 'FALLIDO') phase = 'failed';
  else if (rank >= 5) phase = 'done';
  else if (job.status === 'REQUIERE_INTERVENCION') phase = 'attention';
  else if (rank === 2 && !job.startRequestedAt && !startAsked) phase = 'ready';
  else phase = 'running';

  const waitingForLauncher =
    job.status === 'LISTO_PARA_ROBOT' &&
    job.startRequestedAt !== null &&
    nowMs - Date.parse(job.startRequestedAt) > LAUNCHER_WAIT_HINT_MS;
  const hint = waitingForLauncher
    ? 'El robot todavía no arranca. Abre el lanzador en tu PC (scripts\\lanzador-robot.ps1) y empezará solo.'
    : null;

  return { phase, headline: headlineFor(phase, job, rank, viaApi), stages, hint, latestMessage: latest };
}

/**
 * Los datos salieron de la API de respaldo y no de la Consulta Múltiple de SUNAT: lo dice el historial del backend
 * («Respaldo automático…» o «Datos obtenidos de la API…»). La pantalla no debe contar que lo hizo el robot.
 */
function usedApi(events: RucJobEvent[]): boolean {
  return events.some((event) => /^(Respaldo automático|Datos obtenidos de la )/.test(event.message ?? ''));
}

/** Textos de las etapas cuando los datos vinieron de la API: lo que pasó de verdad. */
function apiDetail(key: FlowStageKey, state: FlowStageState, job: RucJob): string | null {
  if (key === 'sunat' && (state === 'done' || state === 'error')) return 'No se pudo con el robot; se usó la API de respaldo.';
  if (key === 'zip' && state !== 'pending') return 'Sin ZIP: los datos llegaron por API.';
  return stageDetail(key, state, job);
}

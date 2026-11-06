/** Las 5 etapas que ve el usuario, de izquierda a derecha. */
export type FlowStageKey = 'excel' | 'txt' | 'sunat' | 'zip' | 'results';

export type FlowStageState = 'pending' | 'active' | 'done' | 'attention' | 'error';

export interface FlowStage {
  key: FlowStageKey;
  title: string;
  /** Qué hace esta etapa, en palabras del usuario. */
  description: string;
  state: FlowStageState;
  /** Lo que está pasando ahora en esta etapa (solo si tiene algo que contar). */
  detail: string | null;
  /** El robot está ejecutando esta etapa ahora mismo (no solo esperando a que arranque). */
  robotWorking: boolean;
}

/** Momento global del flujo: decide qué botones y avisos se muestran. */
export type FlowPhase = 'idle' | 'ready' | 'running' | 'attention' | 'done' | 'failed';

export interface FlowView {
  phase: FlowPhase;
  headline: string;
  stages: FlowStage[];
  /** Aviso que explica una espera anormal (por ejemplo, el lanzador del robot no está abierto). */
  hint: string | null;
  /** Último mensaje del historial, para mostrarlo como "lo último que pasó". */
  latestMessage: string | null;
}

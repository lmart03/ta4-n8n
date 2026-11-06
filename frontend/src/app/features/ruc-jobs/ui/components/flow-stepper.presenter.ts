import { Injectable } from '@angular/core';
import { FlowStage, FlowStageState } from '../../domain/models/flow.model';

const STATE_LABEL: Record<FlowStageState, string> = {
  pending: 'Pendiente',
  active: 'En curso',
  done: 'Completada',
  attention: 'Necesita tu ayuda',
  error: 'Falló',
};

const CIRCLE_BASE =
  'relative z-10 flex h-8 w-8 items-center justify-center rounded-full border-2 transition-colors duration-300 ease-out';

const CIRCLE: Record<FlowStageState, string> = {
  done: `${CIRCLE_BASE} border-primary bg-primary text-white`,
  active: `${CIRCLE_BASE} border-primary bg-surface text-primary`,
  attention: `${CIRCLE_BASE} border-warning bg-warning text-white`,
  error: `${CIRCLE_BASE} border-danger bg-danger text-white`,
  pending: `${CIRCLE_BASE} border-slate-300 bg-surface text-muted`,
};

const LABEL: Record<FlowStageState, string> = {
  done: 'text-primary',
  active: 'text-primary',
  attention: 'text-warning-text',
  error: 'text-danger-text',
  pending: 'text-muted',
};


/** Pausa visual entre una etapa y la siguiente cuando cambian juntas. */
const STEP_MS = 650;
/** El conector empieza a llenarse un poco después de que su círculo se enciende. */
const CONNECTOR_LEAD_MS = 250;

export interface StagePlan {
  circleMs: number;
  connectorMs: number;
}

/**
 * Lógica de vista del stepper. Todo sale del estado real de cada etapa: ningún temporizador decide cuándo una
 * etapa avanza; las transiciones CSS solo suavizan el cambio cuando el estado llega.
 */
@Injectable()
export class FlowStepperPresenter {
  stateLabel(stage: FlowStage): string {
    if (stage.state === 'active' && stage.key === 'sunat') {
      return stage.robotWorking ? 'El robot está trabajando' : 'Esperando al robot';
    }
    return STATE_LABEL[stage.state];
  }

  labelClasses(state: FlowStageState): string {
    return `text-xs font-medium ${LABEL[state]}`;
  }

  isCurrent(stage: FlowStage): boolean {
    return stage.state === 'active' || stage.state === 'attention';
  }

  circleClasses(state: FlowStageState): string {
    return CIRCLE[state];
  }

  /** Estado con el que se pintó cada etapa la última vez, para saber cuáles cambiaron ahora. */
  private painted = new Map<string, FlowStageState>();

  /**
   * Escalona SOLO lo visual: si varias etapas cambian a la vez (por ejemplo «Excel validado» y «Archivo para SUNAT»
   * llegan juntas), se encienden una tras otra —círculo, luego conector hacia la siguiente— para que el avance se
   * lea de izquierda a derecha. El estado real ya está actualizado; nada se retrasa en el procesamiento.
   * Una etapa que cambia sola (la habitual) se enciende al instante.
   */
  plan(stages: FlowStage[]): StagePlan[] {
    let order = 0;
    const plan = stages.map((stage) => {
      const before = this.painted.get(stage.key) ?? 'pending';
      const changed = stage.state !== before && stage.state !== 'pending';
      const circleMs = changed ? order * STEP_MS : 0;
      if (changed) order += 1;
      return { circleMs, connectorMs: circleMs + CONNECTOR_LEAD_MS };
    });
    this.painted = new Map(stages.map((stage) => [stage.key, stage.state]));
    return plan;
  }

  /** El conector que sale de una etapa se llena cuando esa etapa está completada. */
  connectorFill(state: FlowStageState): number {
    return state === 'done' ? 100 : 0;
  }

  /** Etapa que se resume debajo del stepper en pantallas estrechas. */
  currentStage(stages: FlowStage[]): FlowStage | null {
    return stages.find((stage) => this.isCurrent(stage) || stage.state === 'error') ?? null;
  }
}

import { Injectable } from '@angular/core';
import { ComponentState } from '../../domain/models/bot-status.model';

export interface ConnectionView {
  /** Palabra que acompaña al color: el estado no depende solo del color. */
  word: string;
  badge: string;
  dot: string;
}

const VIEWS: Record<ComponentState, ConnectionView> = {
  OK: { word: 'Conectado', badge: 'bg-success-soft text-success', dot: 'bg-accent' },
  WARN: { word: 'Con aviso', badge: 'bg-warning-soft text-warning-text', dot: 'bg-warning' },
  ERROR: { word: 'Falla', badge: 'bg-danger-soft text-danger-text', dot: 'bg-danger' },
};

@Injectable()
export class ConnectionCardPresenter {
  view(state: ComponentState): ConnectionView {
    return VIEWS[state];
  }
}

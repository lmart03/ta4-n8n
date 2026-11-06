import { Injectable } from '@angular/core';
import { RobotState } from '../../domain/models/robot-status.model';

export interface RobotStatusView {
  title: string;
  detail: string;
  /** Clase del color del punto. */
  dot: string;
}

const VIEWS: Record<RobotState, RobotStatusView> = {
  checking: { title: 'Revisando el robot…', detail: '', dot: 'bg-slate-400' },
  online: { title: 'Robot disponible', detail: 'Listo para consultar.', dot: 'bg-accent' },
  offline: { title: 'Robot no disponible', detail: 'El lanzador no está corriendo en tu PC.', dot: 'bg-warning' },
  unreachable: {
    title: 'Sin conexión con el servidor',
    detail: 'Verifica que Docker y el backend estén en marcha.',
    dot: 'bg-danger',
  },
};

/** Lógica de vista del indicador del robot: qué texto y qué color corresponde a cada estado. */
@Injectable()
export class RobotStatusPresenter {
  /** Enlace que abre el lanzador (lo registra `iniciar.bat /instalar`). */
  readonly startLink = 'ta4robot://start';

  view(state: RobotState): RobotStatusView {
    return VIEWS[state];
  }
}

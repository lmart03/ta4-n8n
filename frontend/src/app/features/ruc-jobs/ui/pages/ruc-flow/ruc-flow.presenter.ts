import { Injectable, inject } from '@angular/core';
import { RucFlowFacade } from '../../../application/services/ruc-flow.facade';

/**
 * Lógica de vista de la pantalla del flujo: conecta la fachada (estado) con la plantilla. La plantilla solo lee
 * de aquí; los detalles de cada sección viven en los presenters de sus componentes.
 */
@Injectable()
export class RucFlowPresenter {
  readonly facade = inject(RucFlowFacade);

  /** La pantalla se abre: empieza a vigilar al robot. */
  initialize(): void {
    this.facade.watchRobot();
  }

  /** La pantalla se cierra: deja de vigilarlo. */
  destroy(): void {
    this.facade.stopWatchingRobot();
  }

  /** Se puede volver a empezar con otro archivo: terminó, o falló y se puede reintentar. */
  canReset(): boolean {
    return this.facade.flow().phase === 'done' || this.facade.canRetry();
  }

  /** Mientras el flujo corre, las celdas sin datos muestran marcador de carga. */
  isWaiting(): boolean {
    const phase = this.facade.flow().phase;
    return phase === 'running' || phase === 'attention';
  }
}

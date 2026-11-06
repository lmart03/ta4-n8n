import { Injectable, inject } from '@angular/core';
import { BotFacade } from '../../../application/services/bot.facade';
import { ComponentState } from '../../../domain/models/bot-status.model';

const DOT: Record<ComponentState, string> = { OK: 'bg-accent', WARN: 'bg-warning', ERROR: 'bg-danger' };

@Injectable()
export class BotLayoutPresenter {
  readonly facade = inject(BotFacade);

  readonly tabs = [
    { path: 'estado', label: 'Estado' },
    { path: 'conversaciones', label: 'Conversaciones' },
    { path: 'pruebas', label: 'Pruebas' },
    { path: 'faqs', label: 'Preguntas frecuentes' },
    { path: 'metricas', label: 'Métricas' },
  ];

  /** Se entra al área del bot: se abre el flujo en vivo y se vigila el estado para el indicador del encabezado. */
  open(): void {
    this.facade.openStream();
    this.facade.watchStatus();
  }

  close(): void {
    this.facade.closeStream();
    this.facade.stopWatchingStatus();
  }

  dot(): string {
    return DOT[this.facade.health().state];
  }
}

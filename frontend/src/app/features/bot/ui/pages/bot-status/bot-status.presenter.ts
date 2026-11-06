import { Injectable, inject } from '@angular/core';
import { BotFacade } from '../../../application/services/bot.facade';
import { ComponentStatus } from '../../../domain/models/bot-status.model';

@Injectable()
export class BotStatusPresenter {
  readonly facade = inject(BotFacade);

  /** Piezas a mostrar; mientras llega la primera respuesta se muestran marcadores en "revisando". */
  components(): ComponentStatus[] | null {
    return this.facade.status()?.components ?? null;
  }

  /** Cuando el túnel está caído el webhook no puede registrarse: el botón se explica en vez de fallar. */
  canRegister(): boolean {
    return this.facade.status()?.tunnelUrl != null;
  }

  needsRegistration(): boolean {
    const webhook = this.components()?.find((c) => c.key === 'webhook');
    return webhook !== undefined && webhook.state !== 'OK';
  }

  checkedAt(): string {
    const status = this.facade.status();
    if (status === null) return '';
    return new Date(status.checkedAt).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false });
  }
}

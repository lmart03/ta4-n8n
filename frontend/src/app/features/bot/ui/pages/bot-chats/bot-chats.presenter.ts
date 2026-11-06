import { Injectable, inject } from '@angular/core';
import { BotFacade } from '../../../application/services/bot.facade';
import { ChatMode } from '../../../domain/models/chat.model';

@Injectable()
export class BotChatsPresenter {
  readonly facade = inject(BotFacade);

  /** En pantallas angostas se ve la lista o el chat, no los dos a la vez. */
  listClass(): string {
    return this.facade.selectedChat() === null ? 'flex' : 'hidden lg:flex';
  }

  threadClass(): string {
    return this.facade.selectedChat() === null ? 'hidden lg:flex' : 'flex';
  }

  streamBanner(): string | null {
    switch (this.facade.stream()) {
      case 'live':
        return null;
      case 'connecting':
        return 'Conectando con el flujo en vivo…';
      case 'reconnecting':
        return 'Se perdió la conexión en vivo. Reconectando; al volver se actualiza la lista.';
    }
  }

  changeMode(mode: ChatMode): void {
    this.facade.setMode(mode);
  }

  /** Recepción responde: el backend lo envía por Telegram y deja el chat en modo humano. */
  send(text: string): void {
    this.facade.reply(text);
  }

  problemText(): string | null {
    const problem = this.facade.chatProblem();
    return problem === null ? null : (problem.detail ?? problem.title ?? 'No se pudo completar la operación.');
  }
}

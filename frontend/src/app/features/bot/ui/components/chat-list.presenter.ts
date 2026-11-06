import { Injectable } from '@angular/core';
import { Chat } from '../../domain/models/chat.model';
import { chatTime, initials } from '../../domain/rules/chat-view.rule';

@Injectable()
export class ChatListPresenter {
  initials(chat: Chat): string {
    return initials(chat.name);
  }

  time(chat: Chat): string {
    return chatTime(chat.lastMessageAt, Date.now());
  }

  /** Línea de vista previa: quién habló por último y qué dijo. */
  preview(chat: Chat): string {
    if (chat.lastText === '') return 'Sin mensajes';
    const prefix = chat.lastAuthor === 'USER' ? '' : chat.lastAuthor === 'HUMANO' ? 'Recepción: ' : 'Bot: ';
    return prefix + chat.lastText.replace(/\s+/g, ' ');
  }

  subtitle(chat: Chat): string {
    if (chat.test) return 'Prueba automática';
    return chat.username ? '@' + chat.username : 'Telegram';
  }

  ariaLabel(chat: Chat, unread: number): string {
    const pending = unread > 0 ? `, ${unread} sin leer` : '';
    return `${chat.name}${pending}${chat.mode === 'HUMANO' ? ', atiende recepción' : ''}`;
  }

  unreadOf(unread: Record<string, number>, id: string): number {
    return unread[id] ?? 0;
  }

  /** Texto escrito en el buscador (el evento viene de un `<input>`). */
  valueOf(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }
}

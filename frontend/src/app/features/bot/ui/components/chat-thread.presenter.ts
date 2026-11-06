import { Injectable, signal } from '@angular/core';
import { ChatMessage } from '../../domain/models/chat.model';
import { dayLabel, initials, messageTime, sourceLabel } from '../../domain/rules/chat-view.rule';

export interface DayGroup {
  label: string;
  items: ChatMessage[];
}

@Injectable()
export class ChatThreadPresenter {
  /** El usuario está leyendo el final de la conversación (o no ha movido el scroll). */
  readonly stickToBottom = signal(true);
  readonly confirmingDelete = signal(false);

  /** Al abrir otro chat se vuelve al final de la conversación y se cierra cualquier confirmación pendiente. */
  resetFor(_chatId: string): void {
    this.stickToBottom.set(true);
    this.confirmingDelete.set(false);
  }

  onScroll(event: Event): void {
    const element = event.target as HTMLElement;
    this.stickToBottom.set(element.scrollHeight - element.scrollTop - element.clientHeight < 80);
  }

  groups(messages: ChatMessage[]): DayGroup[] {
    const now = Date.now();
    const groups: DayGroup[] = [];
    for (const message of messages) {
      const label = dayLabel(message.createdAt, now);
      const last = groups[groups.length - 1];
      if (last?.label === label) last.items.push(message);
      else groups.push({ label, items: [message] });
    }
    return groups;
  }

  /** El usuario habla a la izquierda; el bot y la clínica a la derecha. */
  rowClass(message: ChatMessage): string {
    return message.author === 'USER' ? 'justify-start' : 'justify-end';
  }

  bubbleClass(message: ChatMessage): string {
    switch (message.author) {
      case 'USER':
        return 'bg-white border border-border';
      case 'HUMANO':
        return 'bg-slate-800 text-white';
      default:
        return 'bg-primary-soft border border-emerald-200';
    }
  }

  metaClass(message: ChatMessage): string {
    return message.author === 'HUMANO' ? 'text-slate-300' : 'text-muted';
  }

  time(message: ChatMessage): string {
    return messageTime(message.createdAt);
  }

  author(message: ChatMessage): string {
    if (message.author === 'USER') return '';
    return message.author === 'HUMANO' ? 'Recepción' : 'Bot';
  }

  source(message: ChatMessage): string {
    return message.author === 'BOT' ? sourceLabel(message.source) : '';
  }

  latency(message: ChatMessage): string {
    if (message.author !== 'BOT' || message.latencyMs === null) return '';
    return message.latencyMs >= 1000 ? `${(message.latencyMs / 1000).toFixed(1)} s` : `${message.latencyMs} ms`;
  }

  initials(name: string): string {
    return initials(name);
  }
}

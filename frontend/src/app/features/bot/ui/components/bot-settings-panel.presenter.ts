import { Injectable, signal } from '@angular/core';
import { BotSettings } from '../../domain/models/chat.model';

@Injectable()
export class BotSettingsPanelPresenter {
  readonly retentionOptions = [
    { hours: 1, label: '1 hora' },
    { hours: 6, label: '6 horas' },
    { hours: 24, label: '24 horas' },
    { hours: 72, label: '3 días' },
    { hours: 168, label: '7 días' },
  ];

  readonly store = signal(true);
  readonly hours = signal(24);
  readonly auto = signal(true);

  load(settings: BotSettings | null): void {
    if (settings === null) return;
    this.store.set(settings.storeConversations);
    this.hours.set(settings.retentionHours);
    this.auto.set(settings.autoRegisterWebhook);
  }

  value(): BotSettings {
    return { storeConversations: this.store(), retentionHours: this.hours(), autoRegisterWebhook: this.auto() };
  }

  checked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }

  number(event: Event): number {
    return Number((event.target as HTMLSelectElement).value);
  }
}

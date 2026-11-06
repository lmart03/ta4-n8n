import { Injectable, computed, signal } from '@angular/core';

export const MAX_REPLY_LENGTH = 4000;

@Injectable()
export class ChatComposerPresenter {
  readonly text = signal('');
  readonly length = computed(() => this.text().length);
  readonly tooLong = computed(() => this.length() > MAX_REPLY_LENGTH);
  readonly canSend = computed(() => this.text().trim() !== '' && !this.tooLong());
  readonly max = MAX_REPLY_LENGTH;

  onInput(event: Event): void {
    this.text.set((event.target as HTMLTextAreaElement).value);
  }

  /** Devuelve el texto a enviar y limpia la caja; `null` si no hay nada válido. */
  take(): string | null {
    if (!this.canSend()) return null;
    const value = this.text().trim();
    this.text.set('');
    return value;
  }
}

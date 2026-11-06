import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Chat, ChatMessage, StreamState } from '../../domain/models/chat.model';
import { TestRun } from '../../domain/models/bot-test.model';
import { BotApiService } from './bot-api.service';

export type StreamEvent =
  | { type: 'state'; state: StreamState }
  | { type: 'message'; message: ChatMessage }
  | { type: 'chat'; chat: Chat }
  | { type: 'chat-deleted'; id: string }
  | { type: 'test-progress'; run: TestRun };

const MAX_BACKOFF_MS = 10_000;

/**
 * Flujo en vivo del panel (SSE). Pide un ticket de un solo uso, abre el `EventSource` y, si se corta, reconecta solo
 * con espera creciente (1 s, 2 s… hasta 10 s) pidiendo un ticket nuevo. Al cancelar la suscripción cierra todo.
 */
@Injectable({ providedIn: 'root' })
export class BotStreamService {
  private readonly api = inject(BotApiService);

  connect(): Observable<StreamEvent> {
    return new Observable<StreamEvent>((subscriber) => {
      let source: EventSource | null = null;
      let timer: ReturnType<typeof setTimeout> | undefined;
      let attempts = 0;
      let closed = false;

      const retry = (): void => {
        if (closed) return;
        source?.close();
        source = null;
        subscriber.next({ type: 'state', state: 'reconnecting' });
        attempts += 1;
        timer = setTimeout(open, Math.min(MAX_BACKOFF_MS, 1000 * attempts));
      };

      const open = (): void => {
        this.api.streamTicket().subscribe({
          next: ({ ticket }) => {
            if (closed) return;
            const stream = new EventSource(this.api.streamUrl(ticket));
            source = stream;
            stream.onopen = () => {
              attempts = 0;
              subscriber.next({ type: 'state', state: 'live' });
            };
            stream.onerror = () => retry();
            stream.addEventListener('message', (e) => subscriber.next({ type: 'message', message: parse<ChatMessage>(e) }));
            stream.addEventListener('chat', (e) => subscriber.next({ type: 'chat', chat: parse<Chat>(e) }));
            stream.addEventListener('chat-deleted', (e) => subscriber.next({ type: 'chat-deleted', id: parse<string>(e) }));
            stream.addEventListener('test-progress', (e) => subscriber.next({ type: 'test-progress', run: parse<TestRun>(e) }));
          },
          error: () => retry(),
        });
      };

      subscriber.next({ type: 'state', state: 'connecting' });
      open();

      return () => {
        closed = true;
        if (timer !== undefined) clearTimeout(timer);
        source?.close();
      };
    });
  }
}

function parse<T>(event: Event): T {
  return JSON.parse((event as MessageEvent<string>).data) as T;
}

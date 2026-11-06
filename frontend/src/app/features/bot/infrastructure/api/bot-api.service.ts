import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from 'src/app/core/infrastructure/api/api.service';
import { BotStatus, GeminiPing, WebhookResult } from '../../domain/models/bot-status.model';
import { TestCase, TestRun } from '../../domain/models/bot-test.model';
import { BotSettings, Chat, ChatMessage, ChatMode } from '../../domain/models/chat.model';
import { AdminFaq, BusinessInfo, FaqForm } from '../../domain/models/faq.model';
import { BotMetrics } from '../../domain/models/metrics.model';

/** Acceso HTTP al panel del bot. La clave de administrador la añade el interceptor; aquí no hay lógica de pantalla. */
@Injectable({ providedIn: 'root' })
export class BotApiService {
  private readonly api = inject(ApiService);

  /** Valida la clave (el interceptor ya la envía en la cabecera). */
  session(): Observable<{ ok: boolean }> {
    return this.api.post<{ ok: boolean }>('v1/bot/session', null);
  }

  streamTicket(): Observable<{ ticket: string }> {
    return this.api.post<{ ticket: string }>('v1/bot/stream-ticket', null);
  }

  /** Dirección del flujo en vivo (SSE) para un ticket. */
  streamUrl(ticket: string): string {
    return `${this.api.urlOf('v1/bot/stream')}?ticket=${encodeURIComponent(ticket)}`;
  }

  status(): Observable<BotStatus> {
    return this.api.get<BotStatus>('v1/bot/status');
  }

  registerWebhook(): Observable<WebhookResult> {
    return this.api.post<WebhookResult>('v1/bot/webhook/register', null);
  }

  pingGemini(): Observable<GeminiPing> {
    return this.api.post<GeminiPing>('v1/bot/gemini/ping', null);
  }

  chats(): Observable<Chat[]> {
    return this.api.get<Chat[]>('v1/bot/chats');
  }

  /** Del más nuevo al más viejo; la capa de estado los invierte para mostrarlos como conversación. */
  messages(chatId: string, limit = 100): Observable<ChatMessage[]> {
    return this.api.get<ChatMessage[]>(`v1/bot/chats/${chatId}/messages?limit=${limit}`);
  }

  reply(chatId: string, text: string): Observable<ChatMessage> {
    return this.api.post<ChatMessage>(`v1/bot/chats/${chatId}/messages`, { text });
  }

  setMode(chatId: string, mode: ChatMode): Observable<Chat> {
    return this.api.patch<Chat>(`v1/bot/chats/${chatId}/mode`, { mode });
  }

  deleteChat(chatId: string): Observable<void> {
    return this.api.delete<void>(`v1/bot/chats/${chatId}`);
  }

  settings(): Observable<BotSettings> {
    return this.api.get<BotSettings>('v1/bot/settings');
  }

  saveSettings(settings: BotSettings): Observable<BotSettings> {
    return this.api.put<BotSettings>('v1/bot/settings', settings);
  }

  ask(question: string): Observable<TestCase> {
    return this.api.post<TestCase>('v1/bot/test', { question });
  }

  startSuite(): Observable<TestRun> {
    return this.api.post<TestRun>('v1/bot/test-suite', null);
  }

  lastSuite(): Observable<TestRun> {
    return this.api.get<TestRun>('v1/bot/test-suite');
  }

  metrics(hours: number): Observable<BotMetrics> {
    return this.api.get<BotMetrics>(`v1/bot/metrics?hours=${hours}`);
  }

  faqs(): Observable<AdminFaq[]> {
    return this.api.get<AdminFaq[]>('v1/admin/faqs');
  }

  createFaq(form: FaqForm): Observable<AdminFaq> {
    return this.api.post<AdminFaq>('v1/admin/faqs', form);
  }

  updateFaq(id: string, form: FaqForm): Observable<AdminFaq> {
    return this.api.put<AdminFaq>(`v1/admin/faqs/${id}`, form);
  }

  deleteFaq(id: string): Observable<void> {
    return this.api.delete<void>(`v1/admin/faqs/${id}`);
  }

  business(): Observable<BusinessInfo> {
    return this.api.get<BusinessInfo>('v1/business');
  }

  saveBusiness(info: BusinessInfo): Observable<BusinessInfo> {
    return this.api.put<BusinessInfo>('v1/admin/business', info);
  }
}

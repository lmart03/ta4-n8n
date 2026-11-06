import { createActionGroup, emptyProps, props } from '@ngrx/store';
import { BotStatus, GeminiPing, WebhookResult } from '../../domain/models/bot-status.model';
import { TestCase, TestRun } from '../../domain/models/bot-test.model';
import { BotSettings, Chat, ChatMessage, ChatMode, StreamState } from '../../domain/models/chat.model';
import { AdminFaq, BusinessInfo, FaqForm } from '../../domain/models/faq.model';
import { BotMetrics } from '../../domain/models/metrics.model';
import { ProblemDetails } from '../../domain/models/problem.model';

/** Todo lo que puede pasar en el panel del bot: el usuario, el backend y el flujo en vivo. */
export const BotActions = createActionGroup({
  source: 'Bot',
  events: {
    // Acceso
    'Login Requested': props<{ key: string; redirectTo: string }>(),
    'Login Succeeded': emptyProps(),
    'Login Failed': props<{ problem: ProblemDetails }>(),
    'Logout Requested': emptyProps(),

    // Estado de las piezas (sondeo mientras la pantalla está abierta)
    'Status Watch Started': emptyProps(),
    'Status Watch Stopped': emptyProps(),
    'Status Loaded': props<{ status: BotStatus }>(),
    'Status Failed': props<{ problem: ProblemDetails }>(),
    'Webhook Register Requested': emptyProps(),
    'Webhook Registered': props<{ result: WebhookResult }>(),
    'Webhook Register Failed': props<{ problem: ProblemDetails }>(),
    'Gemini Ping Requested': emptyProps(),
    'Gemini Pinged': props<{ result: GeminiPing }>(),
    'Gemini Ping Failed': props<{ problem: ProblemDetails }>(),

    // Flujo en vivo
    'Stream Started': emptyProps(),
    'Stream Stopped': emptyProps(),
    'Stream State Changed': props<{ state: StreamState }>(),
    'Stream Message Received': props<{ message: ChatMessage }>(),
    'Stream Chat Received': props<{ chat: Chat }>(),
    'Stream Chat Deleted': props<{ id: string }>(),
    'Stream Test Progress': props<{ run: TestRun }>(),

    // Conversaciones
    'Chats Requested': emptyProps(),
    'Chats Loaded': props<{ chats: Chat[] }>(),
    'Chats Failed': props<{ problem: ProblemDetails }>(),
    'Chat Selected': props<{ id: string | null }>(),
    'Messages Loaded': props<{ chatId: string; messages: ChatMessage[] }>(),
    'Chat Search Changed': props<{ text: string }>(),
    'Reply Requested': props<{ chatId: string; text: string }>(),
    'Reply Sent': props<{ message: ChatMessage }>(),
    'Reply Failed': props<{ problem: ProblemDetails }>(),
    'Mode Requested': props<{ chatId: string; mode: ChatMode }>(),
    'Mode Changed': props<{ chat: Chat }>(),
    'Mode Failed': props<{ problem: ProblemDetails }>(),
    'Chat Delete Requested': props<{ chatId: string }>(),
    'Chat Delete Failed': props<{ problem: ProblemDetails }>(),
    'Problem Dismissed': emptyProps(),

    // Ajustes de privacidad
    'Settings Requested': emptyProps(),
    'Settings Loaded': props<{ settings: BotSettings }>(),
    'Settings Save Requested': props<{ settings: BotSettings }>(),
    'Settings Failed': props<{ problem: ProblemDetails }>(),

    // Pruebas
    'Test Suite Requested': emptyProps(),
    'Test Suite Loaded': props<{ run: TestRun }>(),
    'Test Question Asked': props<{ question: string }>(),
    'Test Question Answered': props<{ result: TestCase }>(),
    'Test Failed': props<{ problem: ProblemDetails }>(),
    'Test Last Requested': emptyProps(),

    // FAQ y datos de la clínica
    'Faqs Requested': emptyProps(),
    'Faqs Loaded': props<{ faqs: AdminFaq[]; business: BusinessInfo | null }>(),
    'Faq Edit Started': props<{ id: string | 'new' | null }>(),
    'Faq Save Requested': props<{ id: string | null; form: FaqForm }>(),
    'Faq Saved': props<{ faq: AdminFaq }>(),
    'Faq Toggle Requested': props<{ faq: AdminFaq }>(),
    'Faq Delete Requested': props<{ id: string }>(),
    'Faq Deleted': props<{ id: string }>(),
    'Faq Failed': props<{ problem: ProblemDetails }>(),
    'Business Save Requested': props<{ info: BusinessInfo }>(),
    'Business Saved': props<{ info: BusinessInfo }>(),

    // Métricas
    'Metrics Requested': props<{ hours: number }>(),
    'Metrics Loaded': props<{ metrics: BotMetrics }>(),
    'Metrics Failed': props<{ problem: ProblemDetails }>(),
  },
});

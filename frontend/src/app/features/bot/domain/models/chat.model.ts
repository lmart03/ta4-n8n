export type ChatMode = 'BOT' | 'HUMANO';
export type MessageAuthor = 'USER' | 'BOT' | 'HUMANO';
/** De dónde salió la respuesta del bot. */
export type MessageSource = 'FAQ' | 'IA' | 'CONTINGENCIA' | 'DERIVADA' | 'RECHAZO' | 'AYUDA' | 'HUMANO';

export interface Chat {
  id: string;
  telegramChatId: number;
  name: string;
  username: string | null;
  mode: ChatMode;
  test: boolean;
  lastText: string;
  lastAuthor: MessageAuthor | null;
  lastMessageAt: string;
}

export interface ChatMessage {
  id: string;
  chatId: string;
  author: MessageAuthor;
  text: string;
  source: MessageSource | null;
  latencyMs: number | null;
  createdAt: string;
}

export type StreamState = 'connecting' | 'live' | 'reconnecting';

export interface BotSettings {
  storeConversations: boolean;
  retentionHours: number;
  autoRegisterWebhook: boolean;
}

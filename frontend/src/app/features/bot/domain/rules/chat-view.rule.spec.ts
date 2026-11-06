import { Chat, ChatMessage } from '../models/chat.model';
import { appendMessage, chatTime, dayLabel, filterChats, initials, sourceLabel, upsertChat } from './chat-view.rule';

function chat(id: string, at: string, overrides: Partial<Chat> = {}): Chat {
  return {
    id,
    telegramChatId: 1,
    name: 'Ana Pérez',
    username: null,
    mode: 'BOT',
    test: false,
    lastText: '',
    lastAuthor: null,
    lastMessageAt: at,
    ...overrides,
  };
}

function message(id: string, at: string): ChatMessage {
  return { id, chatId: 'c', author: 'USER', text: 'hola', source: null, latencyMs: null, createdAt: at };
}

describe('chat-view.rule', () => {
  it('saca iniciales de las dos primeras palabras y no falla con nombre vacío', () => {
    expect(initials('ana pérez gómez')).toBe('AP');
    expect(initials('Luis')).toBe('L');
    expect(initials('  ')).toBe('?');
  });

  it('upsertChat reemplaza el chat y deja el más reciente primero', () => {
    const list = [chat('a', '2026-11-06T10:00:00Z'), chat('b', '2026-11-06T09:00:00Z')];
    const updated = upsertChat(list, chat('b', '2026-11-06T11:00:00Z'));
    expect(updated.map((c) => c.id)).toEqual(['b', 'a']);
    expect(updated).toHaveLength(2);
  });

  it('appendMessage no duplica un mensaje que llega por dos caminos (respuesta HTTP y flujo en vivo)', () => {
    const base = [message('1', '2026-11-06T10:00:00Z')];
    expect(appendMessage(base, message('1', '2026-11-06T10:00:00Z'))).toBe(base);
    expect(appendMessage(base, message('2', '2026-11-06T10:01:00Z')).map((m) => m.id)).toEqual(['1', '2']);
  });

  it('appendMessage ordena por hora aunque lleguen desordenados', () => {
    const result = appendMessage([message('2', '2026-11-06T10:05:00Z')], message('1', '2026-11-06T10:01:00Z'));
    expect(result.map((m) => m.id)).toEqual(['1', '2']);
  });

  it('filterChats ignora mayúsculas y tildes y busca también en el último mensaje', () => {
    const list = [chat('a', '2026-11-06T10:00:00Z', { lastText: 'Atienden sábados' }), chat('b', '2026-11-06T09:00:00Z', { name: 'Luis' })];
    expect(filterChats(list, 'SABADOS').map((c) => c.id)).toEqual(['a']);
    expect(filterChats(list, 'luis').map((c) => c.id)).toEqual(['b']);
    expect(filterChats(list, '  ')).toBe(list);
  });

  it('chatTime y dayLabel distinguen hoy, ayer y fechas viejas', () => {
    const now = new Date(2026, 10, 6, 15, 0).getTime();
    expect(chatTime(new Date(2026, 10, 6, 9, 5).toISOString(), now)).toBe('09:05');
    expect(chatTime(new Date(2026, 10, 5, 23, 0).toISOString(), now)).toBe('ayer');
    expect(dayLabel(new Date(2026, 10, 6, 1, 0).toISOString(), now)).toBe('Hoy');
    expect(dayLabel(new Date(2026, 10, 5, 1, 0).toISOString(), now)).toBe('Ayer');
  });

  it('sourceLabel nombra cada fuente en lenguaje del usuario', () => {
    expect(sourceLabel('IA')).toBe('Gemini');
    expect(sourceLabel('DERIVADA')).toBe('Derivada a recepción');
    expect(sourceLabel(null)).toBe('');
  });
});

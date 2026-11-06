import { Chat, ChatMessage } from '../../domain/models/chat.model';
import { AdminFaq } from '../../domain/models/faq.model';
import { BotActions } from './bot.actions';
import { BotState, botFeature, initialBotState } from './bot.feature';

const reducer = botFeature.reducer;

function chat(id: string, at = '2026-11-06T10:00:00Z', overrides: Partial<Chat> = {}): Chat {
  return { id, telegramChatId: 1, name: 'Ana', username: null, mode: 'BOT', test: false, lastText: 'hola', lastAuthor: 'USER', lastMessageAt: at, ...overrides };
}

function message(id: string, chatId: string, author: ChatMessage['author'] = 'USER', at = '2026-11-06T10:00:00Z'): ChatMessage {
  return { id, chatId, author, text: 'texto', source: null, latencyMs: null, createdAt: at };
}

function faq(id: string, sortOrder: number, active = true): AdminFaq {
  return { id, category: 'SERVICIOS', question: 'p', answer: 'r', keywords: 'k', active, sortOrder, updatedAt: null };
}

describe('bot store', () => {
  it('un mensaje de un paciente en un chat que no está abierto suma un "sin leer"', () => {
    const state = reducer(initialBotState, BotActions.streamMessageReceived({ message: message('1', 'a') }));
    expect(state.unread['a']).toBe(1);
  });

  it('el mensaje del chat abierto no cuenta como sin leer y se agrega a la conversación cargada', () => {
    const open: BotState = { ...initialBotState, selectedChatId: 'a', messages: { a: [message('0', 'a')] } };
    const state = reducer(open, BotActions.streamMessageReceived({ message: message('1', 'a', 'USER', '2026-11-06T10:01:00Z') }));
    expect(state.unread['a']).toBeUndefined();
    expect(state.messages['a'].map((m) => m.id)).toEqual(['0', '1']);
  });

  it('lo que escribe la clínica o el bot nunca cuenta como sin leer', () => {
    const state = reducer(initialBotState, BotActions.streamMessageReceived({ message: message('1', 'a', 'BOT') }));
    expect(state.unread['a']).toBeUndefined();
  });

  it('abrir un chat borra sus no leídos', () => {
    const withUnread: BotState = { ...initialBotState, unread: { a: 3, b: 1 } };
    const state = reducer(withUnread, BotActions.chatSelected({ id: 'a' }));
    expect(state.unread).toEqual({ b: 1 });
    expect(state.selectedChatId).toBe('a');
  });

  it('los mensajes cargados llegan del más nuevo al más viejo y se muestran al revés', () => {
    const state = reducer(
      initialBotState,
      BotActions.messagesLoaded({ chatId: 'a', messages: [message('2', 'a', 'BOT', '2026-11-06T10:02:00Z'), message('1', 'a')] }),
    );
    expect(state.messages['a'].map((m) => m.id)).toEqual(['1', '2']);
  });

  it('la respuesta HTTP y el mismo mensaje por el flujo en vivo no se duplican', () => {
    const sent = message('9', 'a', 'HUMANO');
    let state = reducer(initialBotState, BotActions.replySent({ message: sent }));
    state = reducer(state, BotActions.streamMessageReceived({ message: sent }));
    expect(state.messages['a']).toHaveLength(1);
  });

  it('un chat que llega por el flujo se inserta arriba y reemplaza al anterior', () => {
    let state = reducer(initialBotState, BotActions.chatsLoaded({ chats: [chat('a', '2026-11-06T10:00:00Z'), chat('b', '2026-11-06T09:00:00Z')] }));
    state = reducer(state, BotActions.streamChatReceived({ chat: chat('b', '2026-11-06T11:00:00Z', { lastText: 'nuevo' }) }));
    expect(state.chats.map((c) => c.id)).toEqual(['b', 'a']);
    expect(state.chats[0].lastText).toBe('nuevo');
  });

  it('si borran el chat abierto, la selección se limpia', () => {
    const open: BotState = { ...initialBotState, chats: [chat('a')], selectedChatId: 'a', messages: { a: [] } };
    const state = reducer(open, BotActions.streamChatDeleted({ id: 'a' }));
    expect(state.chats).toEqual([]);
    expect(state.selectedChatId).toBeNull();
    expect(state.messages['a']).toBeUndefined();
  });

  it('el resultado de registrar el webhook queda junto al botón, también cuando falla', () => {
    const ok = reducer(initialBotState, BotActions.webhookRegistered({ result: { url: 'u', registered: true, detail: 'Registrado.' } }));
    expect(ok.webhookResult).toEqual({ ok: true, text: 'Registrado.' });
    const bad = reducer(initialBotState, BotActions.webhookRegisterFailed({ problem: { detail: 'Falta el túnel.' } }));
    expect(bad.webhookResult).toEqual({ ok: false, text: 'Falta el túnel.' });
  });

  it('guardar una FAQ nueva la inserta en orden y cierra el formulario; editar la reemplaza', () => {
    const base: BotState = { ...initialBotState, faqs: [faq('a', 1), faq('c', 3)], editingFaq: 'new' };
    const inserted = reducer(base, BotActions.faqSaved({ faq: faq('b', 2) }));
    expect(inserted.faqs.map((f) => f.id)).toEqual(['a', 'b', 'c']);
    expect(inserted.editingFaq).toBeNull();
    const edited = reducer(inserted, BotActions.faqSaved({ faq: faq('b', 2, false) }));
    expect(edited.faqs).toHaveLength(3);
    expect(edited.faqs[1].active).toBe(false);
  });

  it('cerrar sesión limpia todo el estado', () => {
    const dirty: BotState = { ...initialBotState, chats: [chat('a')], unread: { a: 1 } };
    expect(reducer(dirty, BotActions.logoutRequested())).toEqual(initialBotState);
  });

  it('el progreso de las pruebas llega por el flujo y apaga el "iniciando"', () => {
    const starting = reducer(initialBotState, BotActions.testSuiteRequested());
    expect(starting.testStarting).toBe(true);
    const progress = reducer(starting, BotActions.streamTestProgress({ run: { running: true, cases: [], startedAt: null } }));
    expect(progress.testStarting).toBe(false);
    expect(progress.testRun?.running).toBe(true);
  });
});

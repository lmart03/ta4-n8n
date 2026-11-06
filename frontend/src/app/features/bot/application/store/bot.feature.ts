import { createFeature, createReducer, createSelector, on } from '@ngrx/store';
import { ActionResult, BotStatus } from '../../domain/models/bot-status.model';
import { TestCase, TestRun } from '../../domain/models/bot-test.model';
import { BotSettings, Chat, ChatMessage, StreamState } from '../../domain/models/chat.model';
import { AdminFaq, BusinessInfo } from '../../domain/models/faq.model';
import { BotMetrics } from '../../domain/models/metrics.model';
import { ProblemDetails } from '../../domain/models/problem.model';
import { appendMessage, filterChats, upsertChat } from '../../domain/rules/chat-view.rule';
import { overallHealth } from '../../domain/rules/status.rule';
import { BotActions } from './bot.actions';
import { problemText } from './bot.support';

export interface BotState {
  loginBusy: boolean;
  loginProblem: ProblemDetails | null;

  status: BotStatus | null;
  statusProblem: ProblemDetails | null;
  webhookBusy: boolean;
  webhookResult: ActionResult | null;
  geminiBusy: boolean;
  geminiResult: ActionResult | null;

  stream: StreamState;
  chats: Chat[];
  chatsLoaded: boolean;
  selectedChatId: string | null;
  /** Mensajes por chat, de más viejo a más nuevo. */
  messages: Record<string, ChatMessage[]>;
  unread: Record<string, number>;
  chatSearch: string;
  sendBusy: boolean;
  chatProblem: ProblemDetails | null;
  settings: BotSettings | null;

  testRun: TestRun | null;
  testStarting: boolean;
  asking: boolean;
  askResult: TestCase | null;
  testProblem: ProblemDetails | null;

  faqs: AdminFaq[];
  business: BusinessInfo | null;
  faqsLoaded: boolean;
  /** `null`: cerrado; `'new'`: formulario de alta; un id: editando esa pregunta. */
  editingFaq: string | 'new' | null;
  faqBusy: boolean;
  faqProblem: ProblemDetails | null;

  metrics: BotMetrics | null;
  metricsProblem: ProblemDetails | null;
}

export const initialBotState: BotState = {
  loginBusy: false,
  loginProblem: null,
  status: null,
  statusProblem: null,
  webhookBusy: false,
  webhookResult: null,
  geminiBusy: false,
  geminiResult: null,
  stream: 'connecting',
  chats: [],
  chatsLoaded: false,
  selectedChatId: null,
  messages: {},
  unread: {},
  chatSearch: '',
  sendBusy: false,
  chatProblem: null,
  settings: null,
  testRun: null,
  testStarting: false,
  asking: false,
  askResult: null,
  testProblem: null,
  faqs: [],
  business: null,
  faqsLoaded: false,
  editingFaq: null,
  faqBusy: false,
  faqProblem: null,
  metrics: null,
  metricsProblem: null,
};

export const botFeature = createFeature({
  name: 'bot',
  reducer: createReducer(
    initialBotState,
    // Acceso
    on(BotActions.loginRequested, (state) => ({ ...state, loginBusy: true, loginProblem: null })),
    on(BotActions.loginSucceeded, (state) => ({ ...state, loginBusy: false })),
    on(BotActions.loginFailed, (state, { problem }) => ({ ...state, loginBusy: false, loginProblem: problem })),
    on(BotActions.logoutRequested, () => initialBotState),

    // Estado
    on(BotActions.statusLoaded, (state, { status }) => ({ ...state, status, statusProblem: null })),
    on(BotActions.statusFailed, (state, { problem }) => ({ ...state, statusProblem: problem })),
    on(BotActions.webhookRegisterRequested, (state) => ({ ...state, webhookBusy: true, webhookResult: null })),
    on(BotActions.webhookRegistered, (state, { result }) => ({
      ...state,
      webhookBusy: false,
      webhookResult: { ok: true, text: result.detail },
    })),
    on(BotActions.webhookRegisterFailed, (state, { problem }) => ({
      ...state,
      webhookBusy: false,
      webhookResult: { ok: false, text: problemText(problem) },
    })),
    on(BotActions.geminiPingRequested, (state) => ({ ...state, geminiBusy: true, geminiResult: null })),
    on(BotActions.geminiPinged, (state, { result }) => ({
      ...state,
      geminiBusy: false,
      geminiResult: { ok: result.ok, text: result.detail },
    })),
    on(BotActions.geminiPingFailed, (state, { problem }) => ({
      ...state,
      geminiBusy: false,
      geminiResult: { ok: false, text: problemText(problem) },
    })),

    // Flujo en vivo
    on(BotActions.streamStateChanged, (state, { state: stream }) => ({ ...state, stream })),
    on(BotActions.streamChatReceived, (state, { chat }) => ({ ...state, chats: upsertChat(state.chats, chat) })),
    on(BotActions.streamChatDeleted, (state, { id }) => {
      const { [id]: _removed, ...messages } = state.messages;
      return {
        ...state,
        chats: state.chats.filter((chat) => chat.id !== id),
        messages,
        selectedChatId: state.selectedChatId === id ? null : state.selectedChatId,
      };
    }),
    on(BotActions.streamMessageReceived, (state, { message }) => {
      const loaded = state.messages[message.chatId];
      const messages = loaded ? { ...state.messages, [message.chatId]: appendMessage(loaded, message) } : state.messages;
      // Lo que escribió la clínica no cuenta como "sin leer"; tampoco el chat que ya tiene abierto.
      const isIncoming = message.author === 'USER';
      const viewing = state.selectedChatId === message.chatId;
      const unread =
        isIncoming && !viewing ? { ...state.unread, [message.chatId]: (state.unread[message.chatId] ?? 0) + 1 } : state.unread;
      return { ...state, messages, unread };
    }),
    on(BotActions.streamTestProgress, (state, { run }) => ({ ...state, testRun: run, testStarting: false })),

    // Conversaciones
    on(BotActions.chatsLoaded, (state, { chats }) => ({ ...state, chats, chatsLoaded: true })),
    on(BotActions.chatSelected, (state, { id }) => {
      const { [id ?? '']: _read, ...unread } = state.unread;
      return { ...state, selectedChatId: id, unread, chatProblem: null };
    }),
    on(BotActions.messagesLoaded, (state, { chatId, messages }) => ({
      ...state,
      messages: { ...state.messages, [chatId]: [...messages].reverse() },
    })),
    on(BotActions.chatSearchChanged, (state, { text }) => ({ ...state, chatSearch: text })),
    on(BotActions.replyRequested, (state) => ({ ...state, sendBusy: true, chatProblem: null })),
    on(BotActions.replySent, (state, { message }) => ({
      ...state,
      sendBusy: false,
      messages: { ...state.messages, [message.chatId]: appendMessage(state.messages[message.chatId] ?? [], message) },
    })),
    on(BotActions.replyFailed, BotActions.modeFailed, BotActions.chatDeleteFailed, BotActions.chatsFailed, (state, { problem }) => ({
      ...state,
      sendBusy: false,
      chatProblem: problem,
    })),
    on(BotActions.modeChanged, (state, { chat }) => ({ ...state, chats: upsertChat(state.chats, chat) })),
    on(BotActions.problemDismissed, (state) => ({ ...state, chatProblem: null, faqProblem: null, testProblem: null })),
    on(BotActions.settingsLoaded, (state, { settings }) => ({ ...state, settings })),
    on(BotActions.settingsFailed, (state, { problem }) => ({ ...state, chatProblem: problem })),

    // Pruebas
    on(BotActions.testSuiteRequested, (state) => ({ ...state, testStarting: true, testProblem: null })),
    on(BotActions.testSuiteLoaded, (state, { run }) => ({ ...state, testRun: run, testStarting: false })),
    on(BotActions.testQuestionAsked, (state) => ({ ...state, asking: true, askResult: null, testProblem: null })),
    on(BotActions.testQuestionAnswered, (state, { result }) => ({ ...state, asking: false, askResult: result })),
    on(BotActions.testFailed, (state, { problem }) => ({ ...state, asking: false, testStarting: false, testProblem: problem })),

    // FAQ
    on(BotActions.faqsLoaded, (state, { faqs, business }) => ({ ...state, faqs, business, faqsLoaded: true })),
    on(BotActions.faqEditStarted, (state, { id }) => ({ ...state, editingFaq: id, faqProblem: null })),
    on(BotActions.faqSaveRequested, BotActions.faqDeleteRequested, BotActions.faqToggleRequested, BotActions.businessSaveRequested, (state) => ({
      ...state,
      faqBusy: true,
      faqProblem: null,
    })),
    on(BotActions.faqSaved, (state, { faq }) => ({
      ...state,
      faqBusy: false,
      editingFaq: null,
      faqs: state.faqs.some((item) => item.id === faq.id)
        ? state.faqs.map((item) => (item.id === faq.id ? faq : item))
        : [...state.faqs, faq].sort((a, b) => a.sortOrder - b.sortOrder),
    })),
    on(BotActions.faqDeleted, (state, { id }) => ({ ...state, faqBusy: false, faqs: state.faqs.filter((item) => item.id !== id) })),
    on(BotActions.businessSaved, (state, { info }) => ({ ...state, faqBusy: false, business: info })),
    on(BotActions.faqFailed, (state, { problem }) => ({ ...state, faqBusy: false, faqProblem: problem })),

    // Métricas
    on(BotActions.metricsLoaded, (state, { metrics }) => ({ ...state, metrics, metricsProblem: null })),
    on(BotActions.metricsFailed, (state, { problem }) => ({ ...state, metricsProblem: problem })),
  ),
  extraSelectors: ({ selectChats, selectChatSearch, selectSelectedChatId, selectMessages, selectStatus, selectUnread }) => {
    const selectVisibleChats = createSelector(selectChats, selectChatSearch, (chats, text) => filterChats(chats, text));
    const selectSelectedChat = createSelector(selectChats, selectSelectedChatId, (chats, id) => chats.find((chat) => chat.id === id) ?? null);
    return {
      selectVisibleChats,
      selectSelectedChat,
      selectSelectedMessages: createSelector(selectMessages, selectSelectedChatId, (messages, id) => (id ? (messages[id] ?? null) : null)),
      selectHealth: createSelector(selectStatus, (status) => overallHealth(status)),
      selectUnreadTotal: createSelector(selectUnread, (unread) => Object.values(unread).reduce((sum, n) => sum + n, 0)),
    };
  },
});

import { Injectable, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { BotSettings, ChatMode } from '../../domain/models/chat.model';
import { AdminFaq, BusinessInfo, FaqForm } from '../../domain/models/faq.model';
import { BotActions } from '../store/bot.actions';
import { botFeature } from '../store/bot.feature';

/**
 * Puerta de entrada de la UI al estado del bot (NgRx Store): expone el estado como Signals y traduce cada gesto del
 * usuario en una acción. Las pantallas no conocen el Store, HTTP ni las reglas.
 */
@Injectable({ providedIn: 'root' })
export class BotFacade {
  private readonly store = inject(Store);

  // Acceso
  readonly loginBusy = this.store.selectSignal(botFeature.selectLoginBusy);
  readonly loginProblem = this.store.selectSignal(botFeature.selectLoginProblem);

  // Estado
  readonly status = this.store.selectSignal(botFeature.selectStatus);
  readonly statusProblem = this.store.selectSignal(botFeature.selectStatusProblem);
  readonly health = this.store.selectSignal(botFeature.selectHealth);
  readonly webhookBusy = this.store.selectSignal(botFeature.selectWebhookBusy);
  readonly webhookResult = this.store.selectSignal(botFeature.selectWebhookResult);
  readonly geminiBusy = this.store.selectSignal(botFeature.selectGeminiBusy);
  readonly geminiResult = this.store.selectSignal(botFeature.selectGeminiResult);

  // Conversaciones
  readonly stream = this.store.selectSignal(botFeature.selectStream);
  readonly chats = this.store.selectSignal(botFeature.selectVisibleChats);
  readonly chatsLoaded = this.store.selectSignal(botFeature.selectChatsLoaded);
  readonly selectedChat = this.store.selectSignal(botFeature.selectSelectedChat);
  readonly selectedMessages = this.store.selectSignal(botFeature.selectSelectedMessages);
  readonly unread = this.store.selectSignal(botFeature.selectUnread);
  readonly unreadTotal = this.store.selectSignal(botFeature.selectUnreadTotal);
  readonly chatSearch = this.store.selectSignal(botFeature.selectChatSearch);
  readonly sendBusy = this.store.selectSignal(botFeature.selectSendBusy);
  readonly chatProblem = this.store.selectSignal(botFeature.selectChatProblem);
  readonly settings = this.store.selectSignal(botFeature.selectSettings);

  // Pruebas
  readonly testRun = this.store.selectSignal(botFeature.selectTestRun);
  readonly testStarting = this.store.selectSignal(botFeature.selectTestStarting);
  readonly asking = this.store.selectSignal(botFeature.selectAsking);
  readonly askResult = this.store.selectSignal(botFeature.selectAskResult);
  readonly testProblem = this.store.selectSignal(botFeature.selectTestProblem);

  // FAQ y clínica
  readonly faqs = this.store.selectSignal(botFeature.selectFaqs);
  readonly business = this.store.selectSignal(botFeature.selectBusiness);
  readonly faqsLoaded = this.store.selectSignal(botFeature.selectFaqsLoaded);
  readonly editingFaq = this.store.selectSignal(botFeature.selectEditingFaq);
  readonly faqBusy = this.store.selectSignal(botFeature.selectFaqBusy);
  readonly faqProblem = this.store.selectSignal(botFeature.selectFaqProblem);

  // Métricas
  readonly metrics = this.store.selectSignal(botFeature.selectMetrics);
  readonly metricsProblem = this.store.selectSignal(botFeature.selectMetricsProblem);

  login(key: string, redirectTo: string): void {
    if (key.trim() === '' || this.loginBusy()) return;
    this.store.dispatch(BotActions.loginRequested({ key, redirectTo }));
  }

  logout(): void {
    this.store.dispatch(BotActions.logoutRequested());
  }

  watchStatus(): void {
    this.store.dispatch(BotActions.statusWatchStarted());
  }

  stopWatchingStatus(): void {
    this.store.dispatch(BotActions.statusWatchStopped());
  }

  registerWebhook(): void {
    if (!this.webhookBusy()) this.store.dispatch(BotActions.webhookRegisterRequested());
  }

  pingGemini(): void {
    if (!this.geminiBusy()) this.store.dispatch(BotActions.geminiPingRequested());
  }

  openStream(): void {
    this.store.dispatch(BotActions.streamStarted());
  }

  closeStream(): void {
    this.store.dispatch(BotActions.streamStopped());
  }

  loadChats(): void {
    this.store.dispatch(BotActions.chatsRequested());
    this.store.dispatch(BotActions.settingsRequested());
  }

  selectChat(id: string | null): void {
    this.store.dispatch(BotActions.chatSelected({ id }));
  }

  searchChats(text: string): void {
    this.store.dispatch(BotActions.chatSearchChanged({ text }));
  }

  reply(text: string): void {
    const chat = this.selectedChat();
    if (chat === null || this.sendBusy() || text.trim() === '') return;
    this.store.dispatch(BotActions.replyRequested({ chatId: chat.id, text }));
  }

  setMode(mode: ChatMode): void {
    const chat = this.selectedChat();
    if (chat !== null) this.store.dispatch(BotActions.modeRequested({ chatId: chat.id, mode }));
  }

  deleteSelectedChat(): void {
    const chat = this.selectedChat();
    if (chat !== null) this.store.dispatch(BotActions.chatDeleteRequested({ chatId: chat.id }));
  }

  saveSettings(settings: BotSettings): void {
    this.store.dispatch(BotActions.settingsSaveRequested({ settings }));
  }

  dismissProblem(): void {
    this.store.dispatch(BotActions.problemDismissed());
  }

  loadTests(): void {
    this.store.dispatch(BotActions.testLastRequested());
  }

  runSuite(): void {
    if (!this.testStarting() && !this.testRun()?.running) this.store.dispatch(BotActions.testSuiteRequested());
  }

  ask(question: string): void {
    if (question.trim() !== '' && !this.asking()) this.store.dispatch(BotActions.testQuestionAsked({ question: question.trim() }));
  }

  loadFaqs(): void {
    this.store.dispatch(BotActions.faqsRequested());
  }

  editFaq(id: string | 'new' | null): void {
    this.store.dispatch(BotActions.faqEditStarted({ id }));
  }

  saveFaq(id: string | null, form: FaqForm): void {
    this.store.dispatch(BotActions.faqSaveRequested({ id, form }));
  }

  toggleFaq(faq: AdminFaq): void {
    this.store.dispatch(BotActions.faqToggleRequested({ faq }));
  }

  deleteFaq(id: string): void {
    this.store.dispatch(BotActions.faqDeleteRequested({ id }));
  }

  saveBusiness(info: BusinessInfo): void {
    this.store.dispatch(BotActions.businessSaveRequested({ info }));
  }

  loadMetrics(hours: number): void {
    this.store.dispatch(BotActions.metricsRequested({ hours }));
  }
}

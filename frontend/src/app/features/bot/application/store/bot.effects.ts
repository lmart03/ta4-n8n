import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Action } from '@ngrx/store';
import { EMPTY, Observable, of, timer } from 'rxjs';
import { catchError, exhaustMap, map, mergeMap, switchMap, takeUntil, tap } from 'rxjs/operators';
import { AdminSession } from 'src/app/core/auth/admin-session.service';
import { BotApiService } from '../../infrastructure/api/bot-api.service';
import { BotStreamService, StreamEvent } from '../../infrastructure/api/bot-stream.service';
import { BotActions } from './bot.actions';
import { STATUS_POLL_INTERVAL_MS, toProblem } from './bot.support';

type Failure = (props: { problem: ReturnType<typeof toProblem> }) => Action;

/** Ejecuta una llamada y la traduce a su acción de éxito o a la de fallo de su sección. */
function call<T>(request: Observable<T>, success: (value: T) => Action | Action[], failure: Failure): Observable<Action> {
  return request.pipe(
    mergeMap((value) => {
      const result = success(value);
      return Array.isArray(result) ? result : [result];
    }),
    catchError((error: unknown) => of(failure({ problem: toProblem(error) }))),
  );
}

/** Pantalla de acceso: guarda la clave, la valida con el backend y, si no sirve, la olvida. */
export const login$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService), session = inject(AdminSession), router = inject(Router)) =>
    actions$.pipe(
      ofType(BotActions.loginRequested),
      exhaustMap(({ key, redirectTo }) => {
        session.set(key.trim());
        return api.session().pipe(
          map(() => {
            void router.navigateByUrl(redirectTo);
            return BotActions.loginSucceeded();
          }),
          catchError((error: unknown) => {
            session.clear();
            return of(BotActions.loginFailed({ problem: toProblem(error) }));
          }),
        );
      }),
    ),
  { functional: true },
);

export const logout$ = createEffect(
  (actions$ = inject(Actions), session = inject(AdminSession), router = inject(Router)) =>
    actions$.pipe(
      ofType(BotActions.logoutRequested),
      tap(() => {
        session.clear();
        void router.navigate(['/acceso']);
      }),
    ),
  { functional: true, dispatch: false },
);

/** Mientras la pantalla de estado está abierta, revisa las piezas cada 5 s (y de inmediato tras una acción). */
export const statusPoll$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.statusWatchStarted),
      switchMap(() =>
        timer(0, STATUS_POLL_INTERVAL_MS).pipe(
          exhaustMap(() => call(api.status(), (status) => BotActions.statusLoaded({ status }), BotActions.statusFailed)),
          takeUntil(actions$.pipe(ofType(BotActions.statusWatchStopped))),
        ),
      ),
    ),
  { functional: true },
);

export const registerWebhook$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.webhookRegisterRequested),
      exhaustMap(() => call(api.registerWebhook(), (result) => BotActions.webhookRegistered({ result }), BotActions.webhookRegisterFailed)),
    ),
  { functional: true },
);

export const pingGemini$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.geminiPingRequested),
      exhaustMap(() => call(api.pingGemini(), (result) => BotActions.geminiPinged({ result }), BotActions.geminiPingFailed)),
    ),
  { functional: true },
);

function toActions(event: StreamEvent): Action[] {
  switch (event.type) {
    case 'state':
      // Al (re)conectar se vuelve a cargar la lista: pudo haber mensajes mientras no había conexión.
      return event.state === 'live'
        ? [BotActions.streamStateChanged({ state: event.state }), BotActions.chatsRequested()]
        : [BotActions.streamStateChanged({ state: event.state })];
    case 'message':
      return [BotActions.streamMessageReceived({ message: event.message })];
    case 'chat':
      return [BotActions.streamChatReceived({ chat: event.chat })];
    case 'chat-deleted':
      return [BotActions.streamChatDeleted({ id: event.id })];
    case 'test-progress':
      return [BotActions.streamTestProgress({ run: event.run })];
  }
}

/** Flujo en vivo del panel: abierto mientras el usuario está dentro del área del bot. */
export const stream$ = createEffect(
  (actions$ = inject(Actions), stream = inject(BotStreamService)) =>
    actions$.pipe(
      ofType(BotActions.streamStarted),
      switchMap(() =>
        stream.connect().pipe(
          mergeMap(toActions),
          takeUntil(actions$.pipe(ofType(BotActions.streamStopped))),
        ),
      ),
    ),
  { functional: true },
);

export const loadChats$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.chatsRequested),
      switchMap(() => call(api.chats(), (chats) => BotActions.chatsLoaded({ chats }), BotActions.chatsFailed)),
    ),
  { functional: true },
);

/** Al abrir un chat se cargan sus mensajes (los nuevos llegan después por el flujo en vivo). */
export const loadMessages$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.chatSelected),
      switchMap(({ id }) =>
        id === null
          ? EMPTY
          : call(api.messages(id), (messages) => BotActions.messagesLoaded({ chatId: id, messages }), BotActions.chatsFailed),
      ),
    ),
  { functional: true },
);

export const reply$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.replyRequested),
      exhaustMap(({ chatId, text }) => call(api.reply(chatId, text), (message) => BotActions.replySent({ message }), BotActions.replyFailed)),
    ),
  { functional: true },
);

export const changeMode$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.modeRequested),
      mergeMap(({ chatId, mode }) => call(api.setMode(chatId, mode), (chat) => BotActions.modeChanged({ chat }), BotActions.modeFailed)),
    ),
  { functional: true },
);

/** El borrado llega de vuelta por el flujo en vivo (`chat-deleted`), así todos los paneles abiertos se enteran. */
export const deleteChat$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.chatDeleteRequested),
      exhaustMap(({ chatId }) =>
        api.deleteChat(chatId).pipe(
          mergeMap(() => EMPTY),
          catchError((error: unknown) => of(BotActions.chatDeleteFailed({ problem: toProblem(error) }))),
        ),
      ),
    ),
  { functional: true },
);

export const loadSettings$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.settingsRequested),
      switchMap(() => call(api.settings(), (settings) => BotActions.settingsLoaded({ settings }), BotActions.settingsFailed)),
    ),
  { functional: true },
);

export const saveSettings$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.settingsSaveRequested),
      exhaustMap(({ settings }) => call(api.saveSettings(settings), (saved) => BotActions.settingsLoaded({ settings: saved }), BotActions.settingsFailed)),
    ),
  { functional: true },
);

export const startSuite$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.testSuiteRequested),
      exhaustMap(() => call(api.startSuite(), (run) => BotActions.testSuiteLoaded({ run }), BotActions.testFailed)),
    ),
  { functional: true },
);

export const lastSuite$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.testLastRequested),
      switchMap(() => call(api.lastSuite(), (run) => BotActions.testSuiteLoaded({ run }), BotActions.testFailed)),
    ),
  { functional: true },
);

export const askQuestion$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.testQuestionAsked),
      exhaustMap(({ question }) => call(api.ask(question), (result) => BotActions.testQuestionAnswered({ result }), BotActions.testFailed)),
    ),
  { functional: true },
);

export const loadFaqs$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.faqsRequested),
      switchMap(() =>
        call(
          // Los datos de la clínica son opcionales para la tabla: si fallan, la tabla igual se muestra.
          api.faqs().pipe(
            switchMap((faqs) => api.business().pipe(
              map((business) => ({ faqs, business })),
              catchError(() => of({ faqs, business: null })),
            )),
          ),
          ({ faqs, business }) => BotActions.faqsLoaded({ faqs, business }),
          BotActions.faqFailed,
        ),
      ),
    ),
  { functional: true },
);

export const saveFaq$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.faqSaveRequested),
      exhaustMap(({ id, form }) =>
        call(id === null ? api.createFaq(form) : api.updateFaq(id, form), (faq) => BotActions.faqSaved({ faq }), BotActions.faqFailed),
      ),
    ),
  { functional: true },
);

/** Activar/desactivar en la propia tabla: reutiliza el mismo guardado, cambiando solo `active`. */
export const toggleFaq$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.faqToggleRequested),
      exhaustMap(({ faq }) =>
        call(
          api.updateFaq(faq.id, {
            category: faq.category,
            question: faq.question,
            answer: faq.answer,
            keywords: faq.keywords,
            active: !faq.active,
          }),
          (saved) => BotActions.faqSaved({ faq: saved }),
          BotActions.faqFailed,
        ),
      ),
    ),
  { functional: true },
);

export const deleteFaq$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.faqDeleteRequested),
      exhaustMap(({ id }) =>
        call(api.deleteFaq(id).pipe(map(() => id)), (deleted) => BotActions.faqDeleted({ id: deleted }), BotActions.faqFailed),
      ),
    ),
  { functional: true },
);

export const saveBusiness$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.businessSaveRequested),
      exhaustMap(({ info }) => call(api.saveBusiness(info), (saved) => BotActions.businessSaved({ info: saved }), BotActions.faqFailed)),
    ),
  { functional: true },
);

export const loadMetrics$ = createEffect(
  (actions$ = inject(Actions), api = inject(BotApiService)) =>
    actions$.pipe(
      ofType(BotActions.metricsRequested),
      switchMap(({ hours }) => call(api.metrics(hours), (metrics) => BotActions.metricsLoaded({ metrics }), BotActions.metricsFailed)),
    ),
  { functional: true },
);

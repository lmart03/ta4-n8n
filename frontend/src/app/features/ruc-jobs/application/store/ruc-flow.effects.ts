import { inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { Observable, forkJoin, of, timer } from 'rxjs';
import { catchError, exhaustMap, map, switchMap, takeUntil, takeWhile } from 'rxjs/operators';
import { isJobActive } from '../../domain/rules/job-progress.rule';
import { RucJobApiService } from '../../infrastructure/api/ruc-job-api.service';
import { RucFlowActions } from './ruc-flow.actions';
import { POLL_INTERVAL_MS, ROBOT_POLL_INTERVAL_MS, toProblem } from './ruc-flow.support';

/** "Empezar flujo": sube el Excel (el backend lo valida y genera el TXT). */
export const upload$ = createEffect(
  (actions$ = inject(Actions), api = inject(RucJobApiService)) =>
    actions$.pipe(
      ofType(RucFlowActions.begin),
      exhaustMap(({ file }) =>
        api.upload(file).pipe(
          map((job) => RucFlowActions.uploadSucceeded({ job })),
          catchError((error: unknown) => of(RucFlowActions.uploadFailed({ problem: toProblem(error) }))),
        ),
      ),
    ),
  { functional: true },
);

/** Pide el arranque del robot: justo después de subir, y cada vez que el usuario reintenta. */
export const start$ = createEffect(
  (actions$ = inject(Actions), api = inject(RucJobApiService)) =>
    actions$.pipe(
      ofType(RucFlowActions.uploadSucceeded, RucFlowActions.retry),
      map((action) => ('job' in action ? action.job.id : action.jobId)),
      exhaustMap((jobId) =>
        api.start(jobId).pipe(
          map((job) => RucFlowActions.startSucceeded({ job })),
          catchError((error: unknown) => of(RucFlowActions.startFailed({ jobId, problem: toProblem(error) }))),
        ),
      ),
    ),
  { functional: true },
);

/** Plan B: sube a mano el ZIP que el usuario descargó de SUNAT. El backend lo valida (422 si no sirve). */
export const uploadManualResult$ = createEffect(
  (actions$ = inject(Actions), api = inject(RucJobApiService)) =>
    actions$.pipe(
      ofType(RucFlowActions.manualResultSelected),
      exhaustMap(({ jobId, file }) =>
        api.uploadResult(jobId, file).pipe(
          map((job) => RucFlowActions.manualResultUploaded({ job })),
          catchError((error: unknown) => of(RucFlowActions.manualResultFailed({ problem: toProblem(error) }))),
        ),
      ),
    ),
  { functional: true },
);

/** Plan C: el backend consulta los RUC en la API de consultaderuc.pe y completa el trabajo marcando su fuente. */
export const apiLookup$ = createEffect(
  (actions$ = inject(Actions), api = inject(RucJobApiService)) =>
    actions$.pipe(
      ofType(RucFlowActions.apiLookupRequested),
      exhaustMap(({ jobId }) =>
        api.lookupViaApi(jobId).pipe(
          map((job) => RucFlowActions.manualResultUploaded({ job })),
          catchError((error: unknown) => of(RucFlowActions.manualResultFailed({ problem: toProblem(error) }))),
        ),
      ),
    ),
  { functional: true },
);

/**
 * Sigue el trabajo (estado, historial y resultados) hasta que termine. Un fallo de red no corta el sondeo: marca
 * "sin conexión" y la siguiente vuelta lo recupera. Se cancela al volver a empezar.
 */
export const poll$ = createEffect(
  (actions$ = inject(Actions), api = inject(RucJobApiService)) =>
    actions$.pipe(
      ofType(RucFlowActions.startSucceeded, RucFlowActions.startFailed, RucFlowActions.manualResultUploaded),
      map((action) => ('job' in action ? action.job.id : action.jobId)),
      switchMap((jobId) =>
        timer(0, POLL_INTERVAL_MS).pipe(
          switchMap(() => pollOnce(api, jobId)),
          // Termina (incluida la última respuesta) cuando el trabajo ya no está activo.
          takeWhile((action) => !('job' in action) || isJobActive(action.job), true),
          takeUntil(actions$.pipe(ofType(RucFlowActions.reset))),
        ),
      ),
    ),
  { functional: true },
);

function pollOnce(
  api: RucJobApiService,
  jobId: string,
): Observable<ReturnType<typeof RucFlowActions.pollSucceeded> | ReturnType<typeof RucFlowActions.pollFailed>> {
  return forkJoin({ job: api.get(jobId), events: api.events(jobId), results: api.results(jobId) }).pipe(
    map(({ job, events, results }) =>
      RucFlowActions.pollSucceeded({ job, events, items: results.items, now: Date.now() }),
    ),
    catchError(() => of(RucFlowActions.pollFailed())),
  );
}

/** Pregunta cada pocos segundos si el lanzador del robot está escuchando, mientras la pantalla esté abierta. */
export const watchRobot$ = createEffect(
  (actions$ = inject(Actions), api = inject(RucJobApiService)) =>
    actions$.pipe(
      ofType(RucFlowActions.robotWatchStarted),
      switchMap(() =>
        timer(0, ROBOT_POLL_INTERVAL_MS).pipe(
          switchMap(() =>
            api.robotStatus().pipe(
              map((status) => RucFlowActions.robotChecked({ status })),
              catchError(() => of(RucFlowActions.robotChecked({ status: null }))),
            ),
          ),
          takeUntil(actions$.pipe(ofType(RucFlowActions.robotWatchStopped))),
        ),
      ),
    ),
  { functional: true },
);

import { createFeature, createReducer, createSelector, on } from '@ngrx/store';
import { ProblemDetails } from 'src/app/core/infrastructure/api/models/problem-details.model';
import { RobotState, RobotStatus } from '../../domain/models/robot-status.model';
import { RucJobEvent } from '../../domain/models/ruc-job-event.model';
import { RucJobItem } from '../../domain/models/ruc-job-item.model';
import { RucJob } from '../../domain/models/ruc-job.model';
import { deriveFlow } from '../../domain/rules/flow-stages.rule';
import {
  GridSortDirection,
  GridSortKey,
  GridStatusFilter,
  filterByStatus,
  filterItems,
  sortItems,
  summarize,
} from '../../domain/rules/results-grid.rule';
import { RucFlowActions } from './ruc-flow.actions';

export interface RucFlowState {
  /** Datos del Excel elegido (el `File` en sí no se guarda en el estado: no es serializable). */
  fileName: string | null;
  fileSize: number;
  /** Error detectado en el navegador antes de enviar (extensión, tamaño). */
  clientError: string | null;
  job: RucJob | null;
  events: RucJobEvent[];
  items: RucJobItem[];
  /** Error devuelto por el backend como Problem Details (RFC 9457). */
  problem: ProblemDetails | null;
  /** Subiendo el Excel y pidiendo el arranque (todavía no hay trabajo que seguir). */
  busy: boolean;
  /** El sondeo no logra hablar con el backend. */
  offline: boolean;
  /** Hora de la última consulta; las reglas la usan para detectar esperas anormales. */
  now: number;
  search: string;
  statusFilter: GridStatusFilter;
  sortKey: GridSortKey;
  sortDirection: GridSortDirection;
  /** Última respuesta del estado del robot; `null` si el backend no respondió. */
  robotStatus: RobotStatus | null;
  robotChecked: boolean;
}

export const initialRucFlowState: RucFlowState = {
  fileName: null,
  fileSize: 0,
  clientError: null,
  job: null,
  events: [],
  items: [],
  problem: null,
  busy: false,
  offline: false,
  now: 0,
  search: '',
  statusFilter: 'ALL',
  sortKey: 'excelRow',
  sortDirection: 'asc',
  robotStatus: null,
  robotChecked: false,
};

export const rucFlowFeature = createFeature({
  name: 'rucFlow',
  reducer: createReducer(
    initialRucFlowState,
    on(RucFlowActions.fileSelected, (state, { name, size, error }) => ({
      ...state,
      fileName: name,
      fileSize: size,
      clientError: error,
      problem: null,
    })),
    on(RucFlowActions.fileCleared, (state) => ({
      ...state,
      fileName: null,
      fileSize: 0,
      clientError: null,
      problem: null,
    })),
    on(RucFlowActions.begin, RucFlowActions.retry, RucFlowActions.manualResultSelected, RucFlowActions.apiLookupRequested, (state) => ({
      ...state,
      busy: true,
      problem: null,
    })),
    on(RucFlowActions.manualResultUploaded, (state, { job }) => ({ ...state, job, busy: false })),
    on(RucFlowActions.manualResultFailed, (state, { problem }) => ({ ...state, problem, busy: false })),
    on(RucFlowActions.uploadSucceeded, (state, { job }) => ({ ...state, job })),
    on(RucFlowActions.uploadFailed, (state, { problem }) => ({ ...state, problem, busy: false })),
    on(RucFlowActions.startSucceeded, (state, { job }) => ({ ...state, job, busy: false })),
    // El trabajo ya existe: se muestra la grilla con los RUC y se permite reintentar el arranque.
    on(RucFlowActions.startFailed, (state, { problem }) => ({ ...state, problem, busy: false })),
    on(RucFlowActions.pollSucceeded, (state, { job, events, items, now }) => ({
      ...state,
      job,
      events,
      items,
      now,
      offline: false,
    })),
    on(RucFlowActions.pollFailed, (state) => ({ ...state, offline: true })),
    on(RucFlowActions.searchChanged, (state, { text }) => ({ ...state, search: text })),
    on(RucFlowActions.statusFilterChanged, (state, { filter }) => ({ ...state, statusFilter: filter })),
    on(RucFlowActions.sortRequested, (state, { key }) =>
      state.sortKey === key
        ? { ...state, sortDirection: state.sortDirection === 'asc' ? ('desc' as const) : ('asc' as const) }
        : { ...state, sortKey: key, sortDirection: 'asc' as const },
    ),
    on(RucFlowActions.robotChecked, (state, { status }) => ({ ...state, robotStatus: status, robotChecked: true })),
    // Volver a empezar conserva lo que se sabe del robot: sigue siendo el mismo robot.
    on(RucFlowActions.reset, (state) => ({
      ...initialRucFlowState,
      robotStatus: state.robotStatus,
      robotChecked: state.robotChecked,
    })),
  ),
  extraSelectors: ({
    selectJob,
    selectEvents,
    selectNow,
    selectItems,
    selectSearch,
    selectStatusFilter,
    selectSortKey,
    selectSortDirection,
    selectFileName,
    selectClientError,
    selectBusy,
    selectRobotStatus,
    selectRobotChecked,
  }) => {
    const selectFlow = createSelector(selectJob, selectEvents, selectNow, (job, events, now) =>
      deriveFlow(job, events, now),
    );
    return {
      selectFlow,
      selectSummary: createSelector(selectItems, (items) => summarize(items)),
      selectVisibleItems: createSelector(
        selectItems,
        selectSearch,
        selectStatusFilter,
        selectSortKey,
        selectSortDirection,
        (items, search, statusFilter, key, direction) =>
          sortItems(filterItems(filterByStatus(items, statusFilter), search), key, direction),
      ),
      selectCanBegin: createSelector(
        selectFileName,
        selectClientError,
        selectBusy,
        selectJob,
        (fileName, clientError, busy, job) => fileName !== null && clientError === null && !busy && job === null,
      ),
      /** Se puede volver a pedir el arranque: el trabajo falló, o quedó listo sin que el arranque se pidiera. */
      selectCanRetry: createSelector(selectFlow, selectBusy, selectJob, (flow, busy, job) => {
        return !busy && (flow.phase === 'failed' || flow.phase === 'ready') && job !== null;
      }),
      selectRobotState: createSelector(selectRobotChecked, selectRobotStatus, (checked, status): RobotState => {
        if (!checked) return 'checking';
        if (status === null) return 'unreachable';
        return status.online ? 'online' : 'offline';
      }),
    };
  },
});

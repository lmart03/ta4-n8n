import { RucJob } from '../../domain/models/ruc-job.model';
import { RucFlowActions } from './ruc-flow.actions';
import { initialRucFlowState, rucFlowFeature } from './ruc-flow.feature';

const reducer = rucFlowFeature.reducer;

const JOB: RucJob = {
  id: 'c20d5855-f4dd-4c8a-9157-6cfcc2944591',
  status: 'LISTO_PARA_ROBOT',
  total: 2,
  foundCount: 0,
  missingCount: 0,
  unexpectedCount: 0,
  originalFilename: 'ruc.xlsx',
  summary: null,
  createdAt: '2026-11-06T10:00:00Z',
  updatedAt: '2026-11-06T10:00:00Z',
  completedAt: null,
  startRequestedAt: null,
};

describe('rucFlowFeature (reducer)', () => {
  it('guarda solo los datos del archivo elegido y su error de cliente', () => {
    const state = reducer(initialRucFlowState, RucFlowActions.fileSelected({ name: 'a.csv', size: 10, error: 'mal' }));
    expect(state).toMatchObject({ fileName: 'a.csv', fileSize: 10, clientError: 'mal', problem: null });
  });

  it('empezar marca ocupado y limpia el problema; un fallo del arranque libera pero conserva el trabajo', () => {
    let state = reducer(initialRucFlowState, RucFlowActions.uploadSucceeded({ job: JOB }));
    state = reducer(state, RucFlowActions.retry({ jobId: JOB.id }));
    expect(state.busy).toBe(true);

    state = reducer(state, RucFlowActions.startFailed({ jobId: JOB.id, problem: { title: 'No' } }));
    expect(state.busy).toBe(false);
    expect(state.job).toEqual(JOB);
    expect(state.problem?.title).toBe('No');
  });

  it('el sondeo guarda todo y quita el aviso de sin conexión; un fallo lo pone', () => {
    let state = reducer(initialRucFlowState, RucFlowActions.pollFailed());
    expect(state.offline).toBe(true);

    state = reducer(state, RucFlowActions.pollSucceeded({ job: JOB, events: [], items: [], now: 5 }));
    expect(state).toMatchObject({ offline: false, now: 5, job: JOB });
  });

  it('ordenar por la misma columna invierte la dirección y otra columna vuelve a ascendente', () => {
    let state = reducer(initialRucFlowState, RucFlowActions.sortRequested({ key: 'razonSocial' }));
    expect([state.sortKey, state.sortDirection]).toEqual(['razonSocial', 'asc']);
    state = reducer(state, RucFlowActions.sortRequested({ key: 'razonSocial' }));
    expect(state.sortDirection).toBe('desc');
    state = reducer(state, RucFlowActions.sortRequested({ key: 'ruc' }));
    expect([state.sortKey, state.sortDirection]).toEqual(['ruc', 'asc']);
  });

  it('el filtro por estado se guarda y volver a empezar lo limpia', () => {
    let state = reducer(initialRucFlowState, RucFlowActions.statusFilterChanged({ filter: 'PENDIENTE' }));
    expect(state.statusFilter).toBe('PENDIENTE');
    state = reducer(state, RucFlowActions.reset());
    expect(state.statusFilter).toBe('ALL');
  });

  it('volver a empezar limpia todo pero conserva lo que se sabe del robot', () => {
    let state = reducer(initialRucFlowState, RucFlowActions.robotChecked({ status: { online: true, lastSeenAt: null } }));
    state = reducer(state, RucFlowActions.uploadSucceeded({ job: JOB }));
    state = reducer(state, RucFlowActions.reset());

    expect(state.job).toBeNull();
    expect(state.robotChecked).toBe(true);
    expect(state.robotStatus?.online).toBe(true);
  });
});

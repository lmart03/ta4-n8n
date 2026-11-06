import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { provideEffects } from '@ngrx/effects';
import { provideState, provideStore } from '@ngrx/store';
import { RucFlowEffects, rucFlowFeature } from '../store';
import { RucJobItem } from '../../domain/models/ruc-job-item.model';
import { RucJob, RucJobStatus } from '../../domain/models/ruc-job.model';
import { POLL_INTERVAL_MS, ROBOT_POLL_INTERVAL_MS, RucFlowFacade } from './ruc-flow.facade';

const JOB_ID = 'c20d5855-f4dd-4c8a-9157-6cfcc2944591';

function excel(name = 'ruc.xlsx', size = 1000): File {
  return new File([new Uint8Array(size)], name);
}

function job(status: RucJobStatus, overrides: Partial<RucJob> = {}): RucJob {
  return {
    id: JOB_ID,
    status,
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
    ...overrides,
  };
}

const pendingItems: RucJobItem[] = [
  { excelRow: 2, ruc: '20100047218' },
  { excelRow: 3, ruc: '20100053455' },
].map((row) => ({
  ...row,
  status: 'PENDIENTE' as const,
  razonSocial: null,
  direccionFiscal: null,
  condicionContribuyente: null,
  estadoContribuyente: null,
  fechaInicioActividades: null,
  observation: null,
}));

describe('RucFlowFacade', () => {
  let facade: RucFlowFacade;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideStore(),
        provideEffects(),
        provideState(rucFlowFeature),
        provideEffects(RucFlowEffects),
      ],
    });
    facade = TestBed.inject(RucFlowFacade);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    facade.reset();
    http.verify();
  });

  /** Corta el sondeo que sigue activo al final de una prueba con reloj simulado. */
  function endPolling(): void {
    facade.reset();
    discardPeriodicTasks();
  }

  /** Responde una vuelta del sondeo (trabajo, historial y resultados). */
  function answerPoll(current: RucJob, items = pendingItems): void {
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}`).flush(current);
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/events`).flush([]);
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/results`).flush({ jobId: JOB_ID, status: current.status, items });
  }

  it('el estado del robot pasa de "revisando" a listo, desconectado o sin servidor según responda el backend', fakeAsync(() => {
    facade.watchRobot();
    expect(facade.robotState()).toBe('checking');

    tick(0);
    http.expectOne('/api/v1/ruc/robot/status').flush({ online: true, lastSeenAt: '2026-11-06T10:00:00Z' });
    expect(facade.robotState()).toBe('online');

    tick(ROBOT_POLL_INTERVAL_MS);
    http.expectOne('/api/v1/ruc/robot/status').flush({ online: false, lastSeenAt: '2026-11-06T10:00:00Z' });
    expect(facade.robotState()).toBe('offline');

    tick(ROBOT_POLL_INTERVAL_MS);
    http.expectOne('/api/v1/ruc/robot/status').error(new ProgressEvent('error'));
    expect(facade.robotState()).toBe('unreachable');

    facade.stopWatchingRobot();
    discardPeriodicTasks();
  }));

  it('un archivo que no es .xlsx marca error y no deja empezar', () => {
    facade.select(excel('ruc.csv'));
    expect(facade.clientError()).toBe('El archivo debe ser un Excel .xlsx.');
    expect(facade.canBegin()).toBe(false);
    facade.begin();
    http.expectNone(() => true);
  });

  it('un .xlsx válido habilita el botón de empezar', () => {
    facade.select(excel());
    expect(facade.canBegin()).toBe(true);
    expect(facade.flow().phase).toBe('idle');
  });

  it('empezar sube el Excel, pide el arranque y sigue el avance hasta terminar', fakeAsync(() => {
    facade.select(excel());
    facade.begin();
    expect(facade.busy()).toBe(true);

    const upload = http.expectOne('/api/v1/ruc/jobs');
    expect(upload.request.method).toBe('POST');
    expect((upload.request.body as FormData).get('file')).toBeInstanceOf(File);
    upload.flush(job('LISTO_PARA_ROBOT'), { status: 201, statusText: 'Created' });

    const start = http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`);
    expect(start.request.method).toBe('POST');
    start.flush(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:01Z' }), { status: 202, statusText: 'Accepted' });
    expect(facade.busy()).toBe(false);

    tick(0);
    answerPoll(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:01Z' }));
    expect(facade.items()).toHaveLength(2);
    expect(facade.flow().headline).toBe('Esperando al robot…');
    expect(facade.exportUrl()).toBeNull();

    tick(POLL_INTERVAL_MS);
    answerPoll(job('EN_CONSULTA'));
    expect(facade.flow().headline).toBe('El robot está consultando en SUNAT…');

    tick(POLL_INTERVAL_MS);
    const found: RucJobItem[] = pendingItems.map((row) => ({
      ...row,
      status: 'ENCONTRADO' as const,
      razonSocial: `EMPRESA ${row.excelRow}`,
      condicionContribuyente: 'HABIDO',
    }));
    answerPoll(job('COMPLETADO', { foundCount: 2 }), found);
    expect(facade.flow().phase).toBe('done');
    expect(facade.summary().found).toBe(2);
    expect(facade.exportUrl()).toBe(`/api/v1/ruc/jobs/${JOB_ID}/export`);

    // Terminó: ya no se sigue consultando.
    tick(POLL_INTERVAL_MS * 3);
    http.expectNone(() => true);
  }));

  it('un Excel inválido muestra el Problem Details del backend y no crea trabajo', () => {
    facade.select(excel());
    facade.begin();

    http.expectOne('/api/v1/ruc/jobs').flush(
      {
        type: '/problems/ruc-invalid-workbook',
        title: 'Excel de RUC inválido',
        status: 422,
        errors: [{ pointer: '#/rows/4/ruc', code: 'RUC_INVALID_CHECK_DIGIT', detail: 'Fila 4: el dígito verificador no coincide.' }],
      },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    expect(facade.job()).toBeNull();
    expect(facade.busy()).toBe(false);
    expect(facade.problem()?.title).toBe('Excel de RUC inválido');
    expect(facade.problem()?.errors?.[0].detail).toContain('dígito verificador');
    expect(facade.canBegin()).toBe(true);
  });

  it('sin conexión con el backend lo dice claramente', () => {
    facade.select(excel());
    facade.begin();

    http.expectOne('/api/v1/ruc/jobs').error(new ProgressEvent('error'), { status: 0 });

    expect(facade.problem()?.status).toBe(0);
    expect(facade.problem()?.title).toContain('No se pudo contactar');
  });

  it('si el arranque falla, el trabajo y su grilla quedan y se puede reintentar', fakeAsync(() => {
    facade.select(excel());
    facade.begin();
    http.expectOne('/api/v1/ruc/jobs').flush(job('LISTO_PARA_ROBOT'), { status: 201, statusText: 'Created' });
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(
      { type: '/problems/service-unavailable', title: 'Servicio no disponible', status: 503 },
      { status: 503, statusText: 'Service Unavailable' },
    );

    expect(facade.problem()?.title).toBe('Servicio no disponible');
    expect(facade.job()).not.toBeNull();
    tick(0);
    answerPoll(job('LISTO_PARA_ROBOT'));
    expect(facade.flow().phase).toBe('ready');
    expect(facade.canRetry()).toBe(true);

    facade.retry();
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:05Z' }));
    expect(facade.problem()).toBeNull();
    expect(facade.busy()).toBe(false);
    tick(0);
    answerPoll(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:05Z' }));
    expect(facade.flow().phase).toBe('running');
    endPolling();
  }));

  it('un trabajo fallido deja de consultarse, pero reintentar vuelve a seguirlo', fakeAsync(() => {
    facade.select(excel());
    facade.begin();
    http.expectOne('/api/v1/ruc/jobs').flush(job('LISTO_PARA_ROBOT'));
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:01Z' }));
    tick(0);
    answerPoll(job('FALLIDO'));
    expect(facade.flow().phase).toBe('failed');
    tick(POLL_INTERVAL_MS * 2);
    http.expectNone(() => true);

    facade.retry();
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(job('FALLIDO', { startRequestedAt: '2026-11-06T10:01:00Z' }));
    tick(0);
    // Con el reintento pedido, FALLIDO todavía no es definitivo: se sigue consultando.
    answerPoll(job('FALLIDO', { startRequestedAt: '2026-11-06T10:01:00Z' }));
    tick(POLL_INTERVAL_MS);
    answerPoll(job('EN_CONSULTA'));
    expect(facade.flow().phase).toBe('running');
    endPolling();
  }));

  it('marca sin conexión si una consulta falla y se recupera con la siguiente', fakeAsync(() => {
    facade.select(excel());
    facade.begin();
    http.expectOne('/api/v1/ruc/jobs').flush(job('LISTO_PARA_ROBOT'));
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:01Z' }));
    tick(0);
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}`).error(new ProgressEvent('error'), { status: 0 });
    http.match(`/api/v1/ruc/jobs/${JOB_ID}/events`);
    http.match(`/api/v1/ruc/jobs/${JOB_ID}/results`);
    expect(facade.offline()).toBe(true);

    tick(POLL_INTERVAL_MS);
    answerPoll(job('EN_CONSULTA'));
    expect(facade.offline()).toBe(false);
    endPolling();
  }));

  it('plan B: entregar a mano el ZIP de SUNAT termina el trabajo y se ve en la grilla', fakeAsync(() => {
    facade.select(excel());
    facade.begin();
    http.expectOne('/api/v1/ruc/jobs').flush(job('LISTO_PARA_ROBOT'));
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:01Z' }));
    tick(0);
    answerPoll(job('FALLIDO'));
    expect(facade.flow().phase).toBe('failed');
    expect(facade.inputUrl()).toBe(`/api/v1/ruc/jobs/${JOB_ID}/input`);

    facade.uploadManualResult(new File([new Uint8Array(10)], 'RM2026.zip'));
    expect(facade.busy()).toBe(true);
    const upload = http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/result`);
    expect((upload.request.body as FormData).get('file')).toBeInstanceOf(File);
    upload.flush(job('COMPLETADO', { foundCount: 2 }));
    expect(facade.busy()).toBe(false);

    tick(0);
    answerPoll(job('COMPLETADO', { foundCount: 2 }));
    expect(facade.flow().phase).toBe('done');
    endPolling();
  }));

  it('plan B: un ZIP que el backend rechaza muestra el problema y no cambia el trabajo', () => {
    facade.select(excel());
    facade.begin();
    http.expectOne('/api/v1/ruc/jobs').flush(job('LISTO_PARA_ROBOT'));
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/start`).flush(job('LISTO_PARA_ROBOT'));

    facade.uploadManualResult(new File([new Uint8Array(10)], 'malo.zip'));
    http.expectOne(`/api/v1/ruc/jobs/${JOB_ID}/result`).flush(
      { type: '/problems/ruc-invalid-result', title: 'ZIP de resultados inválido', status: 422 },
      { status: 422, statusText: 'Unprocessable Content' },
    );

    expect(facade.problem()?.title).toBe('ZIP de resultados inválido');
    expect(facade.busy()).toBe(false);
    facade.reset();
  });

  it('ordenar por la misma columna invierte la dirección', () => {
    facade.sortBy('razonSocial');
    expect(facade.sortKey()).toBe('razonSocial');
    expect(facade.sortDirection()).toBe('asc');
    facade.sortBy('razonSocial');
    expect(facade.sortDirection()).toBe('desc');
    facade.sortBy('ruc');
    expect(facade.sortDirection()).toBe('asc');
  });

  it('reset deja todo como al inicio', () => {
    facade.select(excel());
    facade.setSearch('banco');
    facade.reset();
    expect(facade.fileName()).toBeNull();
    expect(facade.search()).toBe('');
    expect(facade.job()).toBeNull();
    expect(facade.flow().phase).toBe('idle');
  });
});

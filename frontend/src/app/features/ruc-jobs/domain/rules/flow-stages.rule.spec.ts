import { RucJobEvent } from '../models/ruc-job-event.model';
import { RucJob, RucJobStatus } from '../models/ruc-job.model';
import { FlowStageKey } from '../models/flow.model';
import { LAUNCHER_WAIT_HINT_MS, deriveFlow } from './flow-stages.rule';

const NOW = Date.parse('2026-11-06T10:00:30Z');

function job(status: RucJobStatus, overrides: Partial<RucJob> = {}): RucJob {
  return {
    id: 'abc',
    status,
    total: 20,
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

function event(sequence: number, to: RucJobStatus, message: string | null = null): RucJobEvent {
  return { sequence, occurredAt: '2026-11-06T10:00:00Z', from: null, to, message };
}

function states(status: RucJobStatus, overrides: Partial<RucJob> = {}, events: RucJobEvent[] = []) {
  const flow = deriveFlow(job(status, overrides), events, NOW);
  return Object.fromEntries(flow.stages.map((stage) => [stage.key, stage.state])) as Record<FlowStageKey, string>;
}

describe('deriveFlow', () => {
  it('sin trabajo: todo pendiente y fase idle', () => {
    const flow = deriveFlow(null, [], NOW);
    expect(flow.phase).toBe('idle');
    expect(flow.headline).toBe('Adjunta tu Excel de RUC');
    expect(flow.stages.map((s) => s.key)).toEqual(['excel', 'txt', 'sunat', 'zip', 'results']);
    expect(flow.stages.every((s) => s.state === 'pending')).toBe(true);
  });

  it('mientras se valida el Excel, esa etapa está activa', () => {
    expect(states('VALIDANDO')).toEqual({ excel: 'active', txt: 'pending', sunat: 'pending', zip: 'pending', results: 'pending' });
  });

  it('listo para el robot sin arranque pedido: espera al usuario (fase ready)', () => {
    const flow = deriveFlow(job('LISTO_PARA_ROBOT'), [], NOW);
    expect(flow.phase).toBe('ready');
    expect(flow.stages.map((s) => s.state)).toEqual(['done', 'done', 'pending', 'pending', 'pending']);
  });

  it('con el arranque pedido, la etapa de SUNAT queda activa esperando al robot', () => {
    const flow = deriveFlow(job('LISTO_PARA_ROBOT', { startRequestedAt: '2026-11-06T10:00:25Z' }), [], NOW);
    expect(flow.phase).toBe('running');
    expect(flow.headline).toBe('Esperando al robot…');
    expect(flow.stages[2].state).toBe('active');
    expect(flow.stages[2].detail).toBe('Esperando que el lanzador tome el pedido…');
    expect(flow.hint).toBeNull();
  });

  it('cuando el lanzador ya tomó el pedido, el flujo sigue en marcha y dice que el robot arranca', () => {
    const taken = [{ sequence: 4, occurredAt: '2026-11-06T10:00:26Z', from: 'LISTO_PARA_ROBOT', to: 'LISTO_PARA_ROBOT', message: 'tomó' } as RucJobEvent];
    const flow = deriveFlow(job('LISTO_PARA_ROBOT'), taken, NOW);
    expect(flow.phase).toBe('running');
    expect(flow.stages[2].state).toBe('active');
    expect(flow.stages[2].detail).toBe('El robot está arrancando…');
  });

  it('avisa del lanzador si el robot no arranca pasados unos segundos', () => {
    const asked = new Date(NOW - LAUNCHER_WAIT_HINT_MS - 1000).toISOString();
    const flow = deriveFlow(job('LISTO_PARA_ROBOT', { startRequestedAt: asked }), [], NOW);
    expect(flow.hint).toContain('lanzador');
    expect(flow.hint).toContain('lanzador-robot.ps1');
  });

  it('no avisa del lanzador una vez que el robot ya corre', () => {
    const asked = new Date(NOW - 60_000).toISOString();
    expect(deriveFlow(job('EN_CONSULTA', { startRequestedAt: asked }), [], NOW).hint).toBeNull();
  });

  it('con el robot en SUNAT, esa etapa está activa', () => {
    const flow = deriveFlow(job('EN_CONSULTA'), [], NOW);
    expect(flow.phase).toBe('running');
    expect(flow.headline).toBe('El robot está consultando en SUNAT…');
    expect(flow.stages.map((s) => s.state)).toEqual(['done', 'done', 'active', 'pending', 'pending']);
  });

  it('una verificación de SUNAT pide atención al usuario', () => {
    const flow = deriveFlow(job('REQUIERE_INTERVENCION'), [], NOW);
    expect(flow.phase).toBe('attention');
    expect(flow.headline).toBe('Falta tu ayuda en SUNAT');
    expect(flow.stages[2].state).toBe('attention');
    expect(flow.stages[2].detail).toContain('verificación');
  });

  it('procesando el resultado: SUNAT terminó y se leen los datos', () => {
    expect(states('PROCESANDO_RESULTADO')).toEqual({ excel: 'done', txt: 'done', sunat: 'done', zip: 'active', results: 'pending' });
  });

  it('completado: todo hecho y titular con el conteo', () => {
    const flow = deriveFlow(job('COMPLETADO', { foundCount: 20, summary: '20 de 20 RUC con datos de SUNAT.' }), [], NOW);
    expect(flow.phase).toBe('done');
    expect(flow.headline).toBe('Listo: 20 de 20 RUC con datos de SUNAT');
    expect(flow.stages.every((s) => s.state === 'done')).toBe(true);
    expect(flow.stages[4].detail).toBe('20 de 20 RUC con datos de SUNAT.');
  });

  it('completado con observaciones se distingue en el titular', () => {
    const flow = deriveFlow(job('COMPLETADO_CON_OBSERVACIONES', { foundCount: 18 }), [], NOW);
    expect(flow.phase).toBe('done');
    expect(flow.headline).toBe('Terminó con observaciones: 18 de 20 RUC con datos');
  });

  it('si falla con el robot, el error queda en la etapa de SUNAT con el último mensaje', () => {
    const events = [event(3, 'LISTO_PARA_ROBOT'), event(4, 'EN_CONSULTA'), event(5, 'FALLIDO', 'No se pudo descargar el TXT')];
    const flow = deriveFlow(job('FALLIDO'), events, NOW);
    expect(flow.phase).toBe('failed');
    expect(flow.headline).toBe('El flujo se detuvo');
    expect(flow.stages.map((s) => s.state)).toEqual(['done', 'done', 'error', 'pending', 'pending']);
    expect(flow.stages[2].detail).toBe('Se detuvo en esta etapa.');
    expect(flow.latestMessage).toBe('No se pudo descargar el TXT');
  });

  it('si falla al procesar el resultado, el error queda en la etapa del ZIP', () => {
    const events = [event(4, 'EN_CONSULTA'), event(5, 'PROCESANDO_RESULTADO'), event(6, 'FALLIDO', 'ZIP inválido')];
    const flow = deriveFlow(job('FALLIDO'), events, NOW);
    expect(flow.stages.map((s) => s.state)).toEqual(['done', 'done', 'done', 'error', 'pending']);
  });

  it('si los datos vinieron de la API de respaldo, la pantalla no dice que los trajo el robot', () => {
    const events = [
      event(4, 'EN_CONSULTA'),
      event(5, 'FALLIDO', 'SUNAT rechazó el acceso del robot'),
      event(6, 'PROCESANDO_RESULTADO', 'Respaldo automático: el robot falló y los datos se obtuvieron de la API consultaderuc.pe (no de la Consulta Múltiple de SUNAT).'),
    ];
    const flow = deriveFlow(job('COMPLETADO', { foundCount: 20 }), events, NOW);

    expect(flow.headline).toBe('Listo por API de respaldo: 20 de 20 RUC con datos');
    expect(flow.stages[2].detail).toBe('No se pudo con el robot; se usó la API de respaldo.');
    expect(flow.stages[3].detail).toBe('Sin ZIP: los datos llegaron por API.');
  });

  it('mientras entra el respaldo, el titular no finge que el robot sigue consultando', () => {
    const events = [event(4, 'EN_CONSULTA'), event(5, 'PROCESANDO_RESULTADO', 'Respaldo automático: el robot falló y los datos se obtuvieron de la API consultaderuc.pe')];
    expect(deriveFlow(job('PROCESANDO_RESULTADO'), events, NOW).headline).toBe('El robot no pudo; consultando por la API de respaldo…');
  });

  it('cada etapa tiene título y descripción en lenguaje del usuario', () => {
    const flow = deriveFlow(null, [], NOW);
    for (const stage of flow.stages) {
      expect(stage.title.length).toBeGreaterThan(3);
      expect(stage.description.length).toBeGreaterThan(10);
    }
  });
});

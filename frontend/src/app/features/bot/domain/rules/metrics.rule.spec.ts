import { TestRun } from '../models/bot-test.model';
import { BotMetrics } from '../models/metrics.model';
import { formatLatency, sourceShares, testRunCsv, testSummary } from './metrics.rule';

const metrics: BotMetrics = {
  hours: 24,
  total: 4,
  avgLatencyMs: 400,
  p95LatencyMs: 1000,
  bySource: { FAQ: 2, IA: 1, DERIVADA: 1, RECHAZO: 0 },
  derivedPercent: 25,
};

const run: TestRun = {
  running: false,
  startedAt: null,
  cases: [
    { id: 'PR-BOT-01', question: '¿Atienden "sábados"?', expected: 'x', status: 'PASSED', reply: 'Sí;\nde 9 a 13', source: 'FAQ', latencyMs: 120, detail: null },
    { id: 'PR-BOT-02', question: 'p', expected: 'y', status: 'FAILED', reply: null, source: null, latencyMs: null, detail: 'Inventó un monto.' },
    { id: 'PR-BOT-07', question: null, expected: 'z', status: 'MANUAL', reply: null, source: null, latencyMs: null, detail: null },
  ],
};

describe('metrics.rule', () => {
  it('sourceShares ordena por cantidad, calcula porcentajes y omite fuentes sin respuestas', () => {
    const shares = sourceShares(metrics);
    expect(shares.map((s) => s.source)).toEqual(['FAQ', 'IA', 'DERIVADA']);
    expect(shares[0].percent).toBe(50);
  });

  it('sin respuestas no hay reparto', () => {
    expect(sourceShares({ ...metrics, total: 0 })).toEqual([]);
    expect(sourceShares(null)).toEqual([]);
  });

  it('formatLatency usa ms o s y guion si no hay dato', () => {
    expect(formatLatency(null)).toBe('—');
    expect(formatLatency(850)).toBe('850 ms');
    expect(formatLatency(3199)).toBe('3.2 s');
  });

  it('testSummary cuenta cada estado', () => {
    expect(testSummary(run)).toEqual({ passed: 1, failed: 1, manual: 1, pending: 0 });
    expect(testSummary(null)).toEqual({ passed: 0, failed: 0, manual: 0, pending: 0 });
  });

  it('testRunCsv escapa comillas, saltos de línea y lleva BOM', () => {
    const csv = testRunCsv(run);
    expect(csv.startsWith('﻿')).toBe(true);
    expect(csv).toContain('"¿Atienden ""sábados""?"');
    expect(csv).toContain('"Sí; de 9 a 13"');
    expect(csv.split('\r\n')).toHaveLength(4);
  });
});

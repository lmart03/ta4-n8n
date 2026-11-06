import { BotMetrics } from '../models/metrics.model';
import { TestRun } from '../models/bot-test.model';
import { sourceLabel } from './chat-view.rule';
import { MessageSource } from '../models/chat.model';

export interface SourceShare {
  source: MessageSource;
  label: string;
  count: number;
  percent: number;
}

const ORDER: MessageSource[] = ['FAQ', 'IA', 'DERIVADA', 'RECHAZO', 'CONTINGENCIA', 'AYUDA'];

/** Reparto de respuestas por fuente, de mayor a menor; las fuentes sin respuestas no aparecen. */
export function sourceShares(metrics: BotMetrics | null): SourceShare[] {
  if (metrics === null || metrics.total === 0) return [];
  return ORDER.map((source) => {
    const count = metrics.bySource[source] ?? 0;
    return { source, label: sourceLabel(source), count, percent: Math.round((count * 1000) / metrics.total) / 10 };
  })
    .filter((share) => share.count > 0)
    .sort((a, b) => b.count - a.count);
}

export function formatLatency(ms: number | null): string {
  if (ms === null) return '—';
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)} s` : `${Math.round(ms)} ms`;
}

export interface TestSummary {
  passed: number;
  failed: number;
  manual: number;
  pending: number;
}

export function testSummary(run: TestRun | null): TestSummary {
  const summary: TestSummary = { passed: 0, failed: 0, manual: 0, pending: 0 };
  for (const item of run?.cases ?? []) {
    if (item.status === 'PASSED') summary.passed++;
    else if (item.status === 'FAILED') summary.failed++;
    else if (item.status === 'MANUAL') summary.manual++;
    else summary.pending++;
  }
  return summary;
}

const STATUS_TEXT = { PASSED: 'Pasó', FAILED: 'Falló', MANUAL: 'Manual', PENDING: 'Pendiente', RUNNING: 'Ejecutando' } as const;

/** CSV de los resultados (separador `;`, con BOM para que Excel respete las tildes) para anexar al informe. */
export function testRunCsv(run: TestRun): string {
  const cell = (value: string | number | null): string => `"${String(value ?? '').replace(/"/g, '""').replace(/\s+/g, ' ')}"`;
  const header = ['ID', 'Pregunta', 'Resultado esperado', 'Estado', 'Fuente', 'Latencia (ms)', 'Respuesta', 'Detalle'];
  const rows = run.cases.map((item) =>
    [item.id, item.question, item.expected, STATUS_TEXT[item.status], item.source, item.latencyMs, item.reply, item.detail]
      .map(cell)
      .join(';'),
  );
  return '﻿' + [header.map(cell).join(';'), ...rows].join('\r\n');
}

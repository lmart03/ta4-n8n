import { Injectable, computed, inject, signal } from '@angular/core';
import { BotFacade } from '../../../application/services/bot.facade';
import { TestStatus } from '../../../domain/models/bot-test.model';
import { sourceLabel } from '../../../domain/rules/chat-view.rule';
import { formatLatency, testRunCsv, testSummary } from '../../../domain/rules/metrics.rule';

interface StatusView {
  word: string;
  badge: string;
}

const STATUS: Record<TestStatus, StatusView> = {
  PENDING: { word: 'Pendiente', badge: 'bg-slate-100 text-slate-700' },
  RUNNING: { word: 'Ejecutando…', badge: 'bg-primary-soft text-primary' },
  PASSED: { word: 'Pasó', badge: 'bg-success-soft text-success' },
  FAILED: { word: 'Falló', badge: 'bg-danger-soft text-danger-text' },
  MANUAL: { word: 'Manual', badge: 'bg-warning-soft text-warning-text' },
};

@Injectable()
export class BotTestsPresenter {
  readonly facade = inject(BotFacade);

  readonly question = signal('');
  readonly summary = computed(() => testSummary(this.facade.testRun()));
  readonly running = computed(() => this.facade.testStarting() || this.facade.testRun()?.running === true);

  status(status: TestStatus): StatusView {
    return STATUS[status];
  }

  source(source: Parameters<typeof sourceLabel>[0]): string {
    return sourceLabel(source);
  }

  latency(ms: number | null): string {
    return formatLatency(ms);
  }

  onInput(event: Event): void {
    this.question.set((event.target as HTMLInputElement).value);
  }

  ask(): void {
    this.facade.ask(this.question());
  }

  /** Descarga los resultados como CSV para anexarlos al informe. */
  downloadCsv(): void {
    const run = this.facade.testRun();
    if (run === null) return;
    const blob = new Blob([testRunCsv(run)], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'pruebas-bot-pr-bot.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  problemText(): string | null {
    const problem = this.facade.testProblem();
    return problem === null ? null : (problem.detail ?? problem.title ?? 'No se pudo completar la prueba.');
  }
}

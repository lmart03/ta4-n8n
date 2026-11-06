import { Injectable, computed, inject, signal } from '@angular/core';
import { BotFacade } from '../../../application/services/bot.facade';
import { formatLatency, sourceShares } from '../../../domain/rules/metrics.rule';

@Injectable()
export class BotMetricsPresenter {
  readonly facade = inject(BotFacade);

  readonly ranges = [
    { hours: 1, label: 'Última hora' },
    { hours: 24, label: 'Últimas 24 horas' },
    { hours: 168, label: '7 días' },
  ];
  readonly hours = signal(24);
  readonly shares = computed(() => sourceShares(this.facade.metrics()));

  select(hours: number): void {
    this.hours.set(hours);
    this.facade.loadMetrics(hours);
  }

  latency(ms: number | null | undefined): string {
    return formatLatency(ms ?? null);
  }

  problemText(): string | null {
    const problem = this.facade.metricsProblem();
    return problem === null ? null : (problem.detail ?? problem.title ?? 'No se pudieron cargar las métricas.');
  }
}

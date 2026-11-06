import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { RucJobItem } from '../../domain/models/ruc-job-item.model';
import { GridSortDirection, GridSortKey, GridStatusFilter, GridSummary } from '../../domain/rules/results-grid.rule';
import { ResultsGridPresenter } from './results-grid.presenter';

/**
 * Resultados: métricas, buscador, filtros por estado y la tabla con una fila por RUC. Se muestra desde que el
 * trabajo existe (las filas nacen "pendientes" y se llenan solas cuando llegan los datos).
 */
@Component({
  selector: 'app-results-grid',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ResultsGridPresenter],
  templateUrl: './results-grid.component.html',
})
export class ResultsGridComponent {
  protected readonly presenter = inject(ResultsGridPresenter);

  /** Filas ya filtradas y ordenadas. */
  readonly items = input.required<RucJobItem[]>();
  readonly summary = input.required<GridSummary>();
  readonly search = input('');
  readonly statusFilter = input<GridStatusFilter>('ALL');
  readonly sortKey = input<GridSortKey>('excelRow');
  readonly sortDirection = input<GridSortDirection>('asc');
  /** El flujo está en marcha: las celdas sin datos muestran un marcador de carga en vez de un guion. */
  readonly waiting = input(false);
  /** Descarga del Excel de resultados; solo existe cuando el trabajo terminó. */
  readonly exportUrl = input<string | null>(null);

  readonly searchChange = output<string>();
  readonly statusFilterChange = output<GridStatusFilter>();
  readonly sortChange = output<GridSortKey>();
  readonly filtersCleared = output<void>();
}

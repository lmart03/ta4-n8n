import { Injectable, signal } from '@angular/core';
import { RucItemDetail, RucItemStatus } from '../../domain/models/ruc-job-item.model';
import {
  GridSortDirection,
  GridSortKey,
  GridStatusFilter,
  GridSummary,
  formatIsoDate,
} from '../../domain/rules/results-grid.rule';

export interface GridColumn {
  key: GridSortKey;
  label: string;
}

export interface DetailField {
  label: string;
  value: string;
}

export interface GridMetric {
  label: string;
  value: number;
  /** Qué significa exactamente, para no confundir pendientes, sin datos y errores. */
  hint: string;
  tone: 'neutral' | 'good' | 'warn';
}

export interface GridFilterOption {
  key: GridStatusFilter;
  label: string;
  count: number;
}

const STATUS_LABEL: Record<RucItemStatus, string> = {
  ENCONTRADO: 'Con datos',
  NO_ENCONTRADO: 'Sin datos',
  PENDIENTE: 'Pendiente',
};

const BADGE_BASE = 'inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-medium';

const STATUS_BADGE: Record<RucItemStatus, string> = {
  ENCONTRADO: `${BADGE_BASE} bg-primary-soft text-primary`,
  NO_ENCONTRADO: `${BADGE_BASE} bg-warning-soft text-warning-text`,
  PENDIENTE: `${BADGE_BASE} bg-slate-100 text-slate-600`,
};

/** Lógica de vista de los resultados: métricas, filtros, columnas, orden, fechas y badges. */
@Injectable()
export class ResultsGridPresenter {
  readonly columns: GridColumn[] = [
    { key: 'ruc', label: 'RUC' },
    { key: 'razonSocial', label: 'Razón social' },
    { key: 'direccionFiscal', label: 'Dirección fiscal' },
    { key: 'condicionContribuyente', label: 'Condición' },
    { key: 'fechaInicioActividades', label: 'Inicio de actividades' },
    { key: 'status', label: 'Estado de consulta' },
  ];

  /** Marcador de una celda que aún espera su dato (actividad indeterminada, sin porcentajes inventados). */
  readonly placeholder = 'bg-slate-200 animate-pulse rounded motion-reduce:animate-none';

  readonly secondaryButton =
    'border-slate-300 hover:bg-slate-50 focus-visible:ring-primary inline-flex min-h-11 items-center justify-center gap-2 rounded-lg border px-4 text-sm font-semibold transition-colors focus-visible:ring-2';

  /** Cada indicador significa una sola cosa y los cinco suman los solicitados. */
  metrics(summary: GridSummary): GridMetric[] {
    return [
      { label: 'RUC solicitados', value: summary.total, hint: 'Los del Excel', tone: 'neutral' },
      { label: 'Consultados con datos', value: summary.found, hint: 'SUNAT devolvió información', tone: 'good' },
      { label: 'Habidos', value: summary.habido, hint: 'Con datos y condición HABIDO', tone: 'good' },
      {
        label: 'Sin datos',
        value: summary.missing,
        hint: 'Consultados, pero SUNAT no devolvió información',
        tone: summary.missing > 0 ? 'warn' : 'neutral',
      },
      { label: 'Pendientes', value: summary.pending, hint: 'Aún sin consultar', tone: 'neutral' },
    ];
  }

  filterOptions(summary: GridSummary): GridFilterOption[] {
    return [
      { key: 'ALL', label: 'Todos', count: summary.total },
      { key: 'ENCONTRADO', label: 'Con datos', count: summary.found },
      { key: 'NO_ENCONTRADO', label: 'Sin datos', count: summary.missing },
      { key: 'PENDIENTE', label: 'Pendientes', count: summary.pending },
    ];
  }

  filterClasses(active: boolean): string {
    const base =
      'focus-visible:ring-primary inline-flex min-h-9 items-center gap-1.5 rounded-full border px-3 text-sm font-medium transition-colors focus-visible:ring-2';
    return active
      ? `${base} border-primary bg-primary-soft text-primary`
      : `${base} border-slate-300 bg-surface text-slate-600 hover:bg-slate-50`;
  }

  metricClasses(tone: GridMetric['tone']): string {
    const base = 'rounded-lg border px-4 py-3';
    switch (tone) {
      case 'good':
        return `${base} border-emerald-200 bg-primary-soft`;
      case 'warn':
        return `${base} border-amber-200 bg-warning-soft`;
      default:
        return `${base} border-border bg-surface`;
    }
  }

  metricValueClasses(tone: GridMetric['tone']): string {
    const base = 'text-2xl font-semibold tabular-nums';
    switch (tone) {
      case 'good':
        return `${base} text-primary`;
      case 'warn':
        return `${base} text-warning-text`;
      default:
        return base;
    }
  }

  /** Filas (por número de fila del Excel) con el detalle desplegado. */
  readonly expanded = signal<ReadonlySet<number>>(new Set());

  isOpen(excelRow: number): boolean {
    return this.expanded().has(excelRow);
  }

  toggle(excelRow: number): void {
    this.expanded.update((open) => {
      const next = new Set(open);
      if (!next.delete(excelRow)) next.add(excelRow);
      return next;
    });
  }

  /** Solo los datos adicionales que SUNAT sí informó, con su etiqueta, para mostrarlos al desplegar la fila. */
  detailFields(detail: RucItemDetail | null | undefined): DetailField[] {
    if (!detail) return [];
    const fields: Array<[string, string | null]> = [
      ['Tipo de contribuyente', detail.tipoContribuyente],
      ['Nombre comercial', detail.nombreComercial],
      ['Fecha de inscripción', detail.fechaInscripcion ? formatIsoDate(detail.fechaInscripcion) : null],
      ['Departamento', detail.departamento],
      ['Provincia', detail.provincia],
      ['Distrito', detail.distrito],
      ['Teléfono', detail.telefono],
      ['Actividad principal', detail.actividadPrincipal],
      ['Actividades secundarias', detail.actividadesSecundarias],
      ['Buen contribuyente', detail.buenContribuyente],
      ['Agente de retención', detail.agenteRetencion],
      ['Agente de percepción', detail.agentePercepcion],
    ];
    return fields.filter((field): field is [string, string] => field[1] !== null).map(([label, value]) => ({ label, value }));
  }

  inputValue(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  date(iso: string | null): string {
    return formatIsoDate(iso);
  }

  arrow(active: GridSortKey, direction: GridSortDirection, key: GridSortKey): string {
    if (active !== key) return '';
    return direction === 'asc' ? '▲' : '▼';
  }

  ariaSort(active: GridSortKey, direction: GridSortDirection, key: GridSortKey): 'ascending' | 'descending' | 'none' {
    if (active !== key) return 'none';
    return direction === 'asc' ? 'ascending' : 'descending';
  }

  statusLabel(status: RucItemStatus): string {
    return STATUS_LABEL[status];
  }

  statusBadge(status: RucItemStatus): string {
    return STATUS_BADGE[status];
  }

  conditionBadge(condition: string): string {
    return condition === 'HABIDO'
      ? `${BADGE_BASE} bg-primary-soft text-primary`
      : `${BADGE_BASE} bg-danger-soft text-danger-text`;
  }
}

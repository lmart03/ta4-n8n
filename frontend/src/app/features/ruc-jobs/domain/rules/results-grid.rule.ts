import { RucItemStatus, RucJobItem } from '../models/ruc-job-item.model';

export type GridSortKey =
  | 'excelRow'
  | 'ruc'
  | 'razonSocial'
  | 'direccionFiscal'
  | 'condicionContribuyente'
  | 'fechaInicioActividades'
  | 'status';

export type GridSortDirection = 'asc' | 'desc';

/** Filtro por estado de la consulta; `ALL` no filtra. */
export type GridStatusFilter = 'ALL' | RucItemStatus;

export interface GridSummary {
  total: number;
  /** RUC para los que SUNAT devolvió datos. */
  found: number;
  /** De ellos, los de condición HABIDO. */
  habido: number;
  /** RUC que SUNAT no devolvió. */
  missing: number;
  /** RUC que todavía esperan resultado. */
  pending: number;
}

function normalize(text: string): string {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase();
}

/**
 * Busca en RUC, razón social, dirección y condición, sin distinguir tildes ni mayúsculas. Cada palabra escrita debe
 * aparecer en alguna parte de la fila (no hace falta que estén juntas ni en orden): «banco lima» encuentra al
 * «BANCO DE CREDITO» de Lima.
 */
export function filterItems(items: RucJobItem[], query: string): RucJobItem[] {
  const terms = normalize(query).split(/\s+/).filter((term) => term !== '');
  if (terms.length === 0) return items;
  return items.filter((item) => {
    const haystack = normalize(
      [item.ruc, item.razonSocial, item.direccionFiscal, item.condicionContribuyente]
        .filter((value): value is string => value !== null)
        .join(' '),
    );
    return terms.every((term) => haystack.includes(term));
  });
}

/** Deja solo las filas con el estado pedido (`ALL` las deja todas). */
export function filterByStatus(items: RucJobItem[], filter: GridStatusFilter): RucJobItem[] {
  return filter === 'ALL' ? items : items.filter((item) => item.status === filter);
}

function valueOf(item: RucJobItem, key: GridSortKey): string | number | null {
  return item[key];
}

/** Ordena sin modificar el original; los valores vacíos van siempre al final, sea cual sea la dirección. */
export function sortItems(items: RucJobItem[], key: GridSortKey, direction: GridSortDirection): RucJobItem[] {
  const sign = direction === 'asc' ? 1 : -1;
  return [...items].sort((a, b) => {
    const left = valueOf(a, key);
    const right = valueOf(b, key);
    if (left === null && right === null) return a.excelRow - b.excelRow;
    if (left === null) return 1;
    if (right === null) return -1;
    const order =
      typeof left === 'number' && typeof right === 'number'
        ? left - right
        : String(left).localeCompare(String(right), 'es', { sensitivity: 'base' });
    return order === 0 ? a.excelRow - b.excelRow : order * sign;
  });
}

export function summarize(items: RucJobItem[]): GridSummary {
  return {
    total: items.length,
    found: items.filter((item) => item.status === 'ENCONTRADO').length,
    habido: items.filter((item) => item.status === 'ENCONTRADO' && item.condicionContribuyente === 'HABIDO').length,
    missing: items.filter((item) => item.status === 'NO_ENCONTRADO').length,
    pending: items.filter((item) => item.status === 'PENDIENTE').length,
  };
}

/** `1889-04-09` → `09/04/1889`, sin pasar por `Date` (evita desfases de zona horaria y años anteriores a 1900). */
export function formatIsoDate(iso: string | null): string {
  if (iso === null) return '';
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : iso;
}

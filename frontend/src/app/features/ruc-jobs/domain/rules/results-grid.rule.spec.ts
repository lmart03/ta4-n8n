import { RucJobItem } from '../models/ruc-job-item.model';
import { filterByStatus, filterItems, formatIsoDate, sortItems, summarize } from './results-grid.rule';

describe('filterByStatus', () => {
  const rows: RucJobItem[] = (['ENCONTRADO', 'NO_ENCONTRADO', 'PENDIENTE', 'ENCONTRADO'] as const).map((status, index) => ({
    excelRow: index + 2,
    ruc: `2010004721${index}`,
    status,
    razonSocial: null,
    direccionFiscal: null,
    condicionContribuyente: null,
    estadoContribuyente: null,
    fechaInicioActividades: null,
    observation: null,
  }));

  it('ALL deja todas y un estado deja solo ese', () => {
    expect(filterByStatus(rows, 'ALL')).toHaveLength(4);
    expect(filterByStatus(rows, 'ENCONTRADO').map((r) => r.excelRow)).toEqual([2, 5]);
    expect(filterByStatus(rows, 'PENDIENTE')).toHaveLength(1);
  });
});

function item(excelRow: number, overrides: Partial<RucJobItem> = {}): RucJobItem {
  return {
    excelRow,
    ruc: `2010000000${excelRow}`,
    status: 'ENCONTRADO',
    razonSocial: `EMPRESA ${excelRow}`,
    direccionFiscal: `AV. EJEMPLO ${excelRow}, LIMA`,
    condicionContribuyente: 'HABIDO',
    estadoContribuyente: 'ACTIVO',
    fechaInicioActividades: '2000-01-01',
    observation: null,
    ...overrides,
  };
}

describe('filterItems', () => {
  const items = [
    item(2, { razonSocial: 'UNIÓN DE CERVECERÍAS PERUANAS', direccionFiscal: 'AV. NICOLAS AYLLON, ATE' }),
    item(3, { razonSocial: 'BANCO DE CREDITO', condicionContribuyente: 'NO HABIDO' }),
    item(4, { status: 'PENDIENTE', razonSocial: null, direccionFiscal: null, condicionContribuyente: null }),
  ];

  it('sin texto devuelve todo', () => {
    expect(filterItems(items, '   ')).toHaveLength(3);
  });

  it('ignora tildes y mayúsculas', () => {
    expect(filterItems(items, 'union cerveceria').map((i) => i.excelRow)).toEqual([2]);
  });

  it('busca por RUC, dirección y condición', () => {
    expect(filterItems(items, '20100000003').map((i) => i.excelRow)).toEqual([3]);
    expect(filterItems(items, 'ayllon').map((i) => i.excelRow)).toEqual([2]);
    expect(filterItems(items, 'no habido').map((i) => i.excelRow)).toEqual([3]);
  });

  it('las filas pendientes (sin datos) no rompen la búsqueda', () => {
    expect(filterItems(items, 'banco')).toHaveLength(1);
    expect(filterItems(items, 'zzzz')).toEqual([]);
  });
});

describe('sortItems', () => {
  const items = [
    item(2, { razonSocial: 'ZETA', fechaInicioActividades: '1990-05-01' }),
    item(3, { razonSocial: 'ALFA', fechaInicioActividades: null }),
    item(4, { razonSocial: 'ÁNGEL', fechaInicioActividades: '1889-04-09' }),
  ];

  it('ordena texto ignorando tildes', () => {
    expect(sortItems(items, 'razonSocial', 'asc').map((i) => i.razonSocial)).toEqual(['ALFA', 'ÁNGEL', 'ZETA']);
    expect(sortItems(items, 'razonSocial', 'desc').map((i) => i.razonSocial)).toEqual(['ZETA', 'ÁNGEL', 'ALFA']);
  });

  it('los vacíos van siempre al final, en ambas direcciones', () => {
    expect(sortItems(items, 'fechaInicioActividades', 'asc').map((i) => i.excelRow)).toEqual([4, 2, 3]);
    expect(sortItems(items, 'fechaInicioActividades', 'desc').map((i) => i.excelRow)).toEqual([2, 4, 3]);
  });

  it('ordena números por valor', () => {
    expect(sortItems(items, 'excelRow', 'desc').map((i) => i.excelRow)).toEqual([4, 3, 2]);
  });

  it('no modifica el arreglo original', () => {
    const copy = [...items];
    sortItems(items, 'razonSocial', 'asc');
    expect(items).toEqual(copy);
  });
});

describe('summarize', () => {
  it('cuenta encontrados, habidos, sin datos y pendientes', () => {
    const summary = summarize([
      item(2),
      item(3, { condicionContribuyente: 'NO HABIDO' }),
      item(4, { status: 'NO_ENCONTRADO' }),
      item(5, { status: 'PENDIENTE' }),
    ]);
    expect(summary).toEqual({ total: 4, found: 2, habido: 1, missing: 1, pending: 1 });
  });
});

describe('formatIsoDate', () => {
  it('convierte yyyy-MM-dd a dd/MM/yyyy sin desfases', () => {
    expect(formatIsoDate('1889-04-09')).toBe('09/04/1889');
    expect(formatIsoDate('2010-06-22')).toBe('22/06/2010');
  });

  it('devuelve vacío para nulo y deja pasar lo que no reconoce', () => {
    expect(formatIsoDate(null)).toBe('');
    expect(formatIsoDate('ayer')).toBe('ayer');
  });
});

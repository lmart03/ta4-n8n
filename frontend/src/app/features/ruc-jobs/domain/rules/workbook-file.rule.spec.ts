import { MAX_WORKBOOK_BYTES, validateWorkbookFile } from './workbook-file.rule';

describe('validateWorkbookFile', () => {
  it('acepta un .xlsx dentro del límite', () => {
    expect(validateWorkbookFile({ name: 'ruc_entrada.xlsx', size: 12_000 })).toBeNull();
  });

  it('ignora mayúsculas en la extensión', () => {
    expect(validateWorkbookFile({ name: 'RUC.XLSX', size: 1 })).toBeNull();
  });

  it('rechaza otras extensiones', () => {
    expect(validateWorkbookFile({ name: 'ruc.csv', size: 10 })).toBe('El archivo debe ser un Excel .xlsx.');
    expect(validateWorkbookFile({ name: 'ruc.xls', size: 10 })).toBe('El archivo debe ser un Excel .xlsx.');
  });

  it('rechaza archivos vacíos', () => {
    expect(validateWorkbookFile({ name: 'ruc.xlsx', size: 0 })).toBe('El archivo está vacío.');
  });

  it('rechaza archivos que superan el límite', () => {
    expect(validateWorkbookFile({ name: 'ruc.xlsx', size: MAX_WORKBOOK_BYTES + 1 })).toBe(
      'El archivo supera el límite de 2 MB.',
    );
    expect(validateWorkbookFile({ name: 'ruc.xlsx', size: MAX_WORKBOOK_BYTES })).toBeNull();
  });
});

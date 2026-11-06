/** Tamaño máximo del Excel: el mismo límite que aplica el backend. */
export const MAX_WORKBOOK_BYTES = 2_000_000;

export const WORKBOOK_EXTENSION = '.xlsx';

/** Forma mínima de un archivo para validarlo sin depender del `File` del navegador. */
export interface WorkbookCandidate {
  name: string;
  size: number;
}

/**
 * Valida en el cliente el Excel antes de enviarlo. Devuelve el mensaje de error o `null` si es válido.
 * El backend vuelve a validar todo: esto solo evita una ida y vuelta inútil.
 */
export function validateWorkbookFile(file: WorkbookCandidate): string | null {
  if (!file.name.toLowerCase().endsWith(WORKBOOK_EXTENSION)) {
    return 'El archivo debe ser un Excel .xlsx.';
  }
  if (file.size === 0) {
    return 'El archivo está vacío.';
  }
  if (file.size > MAX_WORKBOOK_BYTES) {
    return 'El archivo supera el límite de 2 MB.';
  }
  return null;
}

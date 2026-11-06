/** Estado de un RUC dentro del trabajo. */
export type RucItemStatus = 'PENDIENTE' | 'ENCONTRADO' | 'NO_ENCONTRADO';

/** Una fila de la grilla: un RUC solicitado y lo que SUNAT devolvió para él. */
export interface RucJobItem {
  excelRow: number;
  ruc: string;
  status: RucItemStatus;
  razonSocial: string | null;
  direccionFiscal: string | null;
  /** HABIDO o NO HABIDO (la "Condición del Contribuyente"; no es el estado ACTIVO/BAJA). */
  condicionContribuyente: string | null;
  estadoContribuyente: string | null;
  /** Fecha ISO `yyyy-MM-dd`. */
  fechaInicioActividades: string | null;
  observation: string | null;
  /** Datos adicionales de SUNAT; `null` mientras el RUC no tenga datos. */
  detalle?: RucItemDetail | null;
}

/** Lo que SUNAT informa además de lo básico; cada dato puede faltar. Las fechas son ISO `yyyy-MM-dd`. */
export interface RucItemDetail {
  tipoContribuyente: string | null;
  nombreComercial: string | null;
  fechaInscripcion: string | null;
  departamento: string | null;
  provincia: string | null;
  distrito: string | null;
  telefono: string | null;
  actividadPrincipal: string | null;
  actividadesSecundarias: string | null;
  buenContribuyente: string | null;
  agenteRetencion: string | null;
  agentePercepcion: string | null;
}

export interface RucJobResults {
  jobId: string;
  status: string;
  items: RucJobItem[];
}

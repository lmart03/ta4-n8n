import { Injectable } from '@angular/core';
import { RucJob } from '../../domain/models/ruc-job.model';
import { WORKBOOK_EXTENSION } from '../../domain/rules/workbook-file.rule';

/** Estado de la tarjeta del archivo, de menos a más avanzado. */
export type FilePanelState = 'empty' | 'invalid' | 'ready' | 'validating' | 'validated';

export interface FilePanelInputs {
  fileName: string | null;
  clientError: string | null;
  busy: boolean;
  job: RucJob | null;
}

/** Lógica de vista de la tarjeta del archivo: en qué estado está y cómo se ve cada uno. */
@Injectable()
export class FilePanelPresenter {
  readonly accept = WORKBOOK_EXTENSION;

  readonly primaryButton =
    'bg-primary hover:bg-primary-hover focus-visible:ring-primary inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-5 font-semibold text-white transition-colors focus-visible:ring-2 focus-visible:ring-offset-2 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50';
  readonly secondaryButton =
    'border-slate-300 hover:bg-slate-50 focus-visible:ring-primary inline-flex min-h-11 items-center justify-center rounded-lg border px-4 text-sm font-semibold transition-colors focus-visible:ring-2 disabled:cursor-not-allowed disabled:opacity-50';

  state(inputs: FilePanelInputs): FilePanelState {
    if (inputs.job !== null) return 'validated';
    if (inputs.fileName === null) return 'empty';
    if (inputs.clientError !== null) return 'invalid';
    return inputs.busy ? 'validating' : 'ready';
  }

  cardClasses(state: FilePanelState): string {
    const base = 'flex flex-wrap items-center gap-4 rounded-lg border p-4 transition-colors duration-300 animate-rise';
    return state === 'invalid' ? `${base} border-red-200 bg-danger-soft` : `${base} border-border bg-surface`;
  }

  iconClasses(state: FilePanelState): string {
    return state === 'invalid'
      ? 'flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-red-100 text-danger'
      : 'flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-primary-soft text-primary';
  }

  formatSize(bytes: number): string {
    return bytes < 1024 ? `${bytes} B` : `${(bytes / 1024).toFixed(1)} KB`;
  }

  shortId(id: string): string {
    return id.slice(0, 8);
  }

  /** Devuelve el archivo elegido y deja el input listo para elegir el mismo otra vez. */
  picked(event: Event): File | null {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    return file;
  }
}

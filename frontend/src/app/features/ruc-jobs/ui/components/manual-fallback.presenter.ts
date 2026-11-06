import { Injectable } from '@angular/core';

/** Lógica de vista del plan B manual: dónde consultar y cómo se ve cada acción. */
@Injectable()
export class ManualFallbackPresenter {
  /** Página oficial de la Consulta Múltiple, para abrirla en el navegador del usuario (no en el del robot). */
  readonly sunatUrl = 'https://e-consultaruc.sunat.gob.pe/cl-ti-itmrconsmulruc/jrmS00Alias';
  readonly zipAccept = '.zip';

  readonly linkButton =
    'border-slate-300 hover:bg-slate-50 focus-visible:ring-primary inline-flex min-h-11 items-center justify-center rounded-lg border px-4 text-sm font-semibold transition-colors focus-visible:ring-2';
  readonly primaryButton =
    'bg-primary hover:bg-primary-hover focus-visible:ring-primary inline-flex min-h-11 cursor-pointer items-center justify-center gap-2 rounded-lg px-4 text-sm font-semibold text-white transition-colors focus-within:ring-2 focus-within:ring-offset-2';

  /** Devuelve el ZIP elegido y deja el input listo para elegir el mismo otra vez. */
  picked(event: Event): File | null {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    return file;
  }
}

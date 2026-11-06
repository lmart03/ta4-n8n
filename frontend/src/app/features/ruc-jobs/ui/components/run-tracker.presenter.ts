import { Injectable } from '@angular/core';
import { FailureExplanation, explainFailure } from '../../domain/rules/failure-explanation.rule';

/** Lógica de vista del seguimiento: explicaciones de los fallos y estilos de los avisos. */
@Injectable()
export class RunTrackerPresenter {
  readonly primaryButton =
    'bg-primary hover:bg-primary-hover focus-visible:ring-primary inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-5 font-semibold text-white transition-colors focus-visible:ring-2 focus-visible:ring-offset-2 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50';

  /** Explicación comprensible del fallo; el mensaje técnico original queda aparte, en un apartado expandible. */
  explain(latestMessage: string | null): FailureExplanation {
    return explainFailure(latestMessage);
  }
}

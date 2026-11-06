import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ProblemDetails } from '../../domain/models/problem.model';
import { FlowView } from '../../domain/models/flow.model';
import { FlowStepperComponent } from './flow-stepper.component';
import { ManualFallbackComponent } from './manual-fallback.component';
import { RunTrackerPresenter } from './run-tracker.presenter';

/**
 * Seguimiento de la ejecución: el stepper de las cinco etapas y, debajo, lo que el usuario necesita saber o hacer
 * (aviso de espera, verificación de SUNAT, fallo con reintento, o consulta completada con la descarga).
 * Todo sale del estado real de la ejecución; no simula avance.
 */
@Component({
  selector: 'app-run-tracker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FlowStepperComponent, ManualFallbackComponent],
  providers: [RunTrackerPresenter],
  templateUrl: './run-tracker.component.html',
})
export class RunTrackerComponent {
  protected readonly presenter = inject(RunTrackerPresenter);

  readonly flow = input.required<FlowView>();
  /** Error del backend al pedir el arranque (distinto de un fallo del robot). */
  readonly problem = input<ProblemDetails | null>(null);
  readonly canRetry = input(false);
  readonly summary = input<string | null>(null);
  readonly exportUrl = input<string | null>(null);
  /** Descarga del TXT y estado de carga, para el plan B manual. */
  readonly inputUrl = input<string | null>(null);
  readonly busy = input(false);

  readonly retry = output<void>();
  readonly manualResult = output<File>();
  readonly apiLookup = output<void>();
}

import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { FlowStage } from '../../domain/models/flow.model';
import { FlowStepperPresenter } from './flow-stepper.presenter';

/** Las etapas del flujo, una a una. */
@Component({
  selector: 'app-flow-stepper',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [FlowStepperPresenter],
  templateUrl: './flow-stepper.component.html',
})
export class FlowStepperComponent {
  protected readonly presenter = inject(FlowStepperPresenter);

  readonly stages = input.required<FlowStage[]>();

  /** Retrasos visuales de cada etapa: si varias cambian a la vez, se encienden una tras otra. */
  protected readonly plan = computed(() => this.presenter.plan(this.stages()));
}

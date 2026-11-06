import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RobotState } from '../../domain/models/robot-status.model';
import { RobotStatusPresenter } from './robot-status.presenter';

/** Estado del robot: un punto de color y dos líneas de texto (el estado no depende solo del color). */
@Component({
  selector: 'app-robot-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [RobotStatusPresenter],
  templateUrl: './robot-status.component.html',
})
export class RobotStatusComponent {
  protected readonly presenter = inject(RobotStatusPresenter);

  readonly state = input.required<RobotState>();
}

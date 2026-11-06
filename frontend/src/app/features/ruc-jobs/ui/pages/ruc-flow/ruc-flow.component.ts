import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FilePanelComponent } from '../../components/file-panel.component';
import { ResultsGridComponent } from '../../components/results-grid.component';
import { RobotStatusComponent } from '../../components/robot-status.component';
import { RunTrackerComponent } from '../../components/run-tracker.component';
import { RucFlowPresenter } from './ruc-flow.presenter';

/**
 * Pantalla del flujo completo, en cuatro secciones: encabezado con la disponibilidad del robot, gestión del
 * archivo, seguimiento de la ejecución y resultados. Solo habla con su presenter (que usa la fachada).
 */
@Component({
  selector: 'app-ruc-flow',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FilePanelComponent, ResultsGridComponent, RobotStatusComponent, RunTrackerComponent],
  providers: [RucFlowPresenter],
  templateUrl: './ruc-flow.component.html',
})
export class RucFlowComponent implements OnInit, OnDestroy {
  protected readonly presenter = inject(RucFlowPresenter);

  ngOnInit(): void {
    this.presenter.initialize();
  }

  ngOnDestroy(): void {
    this.presenter.destroy();
  }
}

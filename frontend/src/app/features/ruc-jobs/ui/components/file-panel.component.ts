import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ProblemDetails } from '../../domain/models/problem.model';
import { FileUploadComponent } from 'src/app/shared/components/file-upload/file-upload.component';
import { RobotState } from '../../domain/models/robot-status.model';
import { RucJob } from '../../domain/models/ruc-job.model';
import { FilePanelPresenter } from './file-panel.presenter';

/**
 * Gestión del archivo: zona de carga cuando no hay archivo, y una tarjeta compacta con su estado (válido,
 * validando, inválido o validado por el backend) más la acción principal para iniciar la consulta.
 */
@Component({
  selector: 'app-file-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FileUploadComponent],
  providers: [FilePanelPresenter],
  templateUrl: './file-panel.component.html',
})
export class FilePanelComponent {
  protected readonly presenter = inject(FilePanelPresenter);

  readonly fileName = input<string | null>(null);
  readonly fileSize = input(0);
  /** Error detectado en el navegador antes de enviar. */
  readonly clientError = input<string | null>(null);
  /** Error del backend al validar el Excel (Problem Details). */
  readonly problem = input<ProblemDetails | null>(null);
  readonly busy = input(false);
  readonly canBegin = input(false);
  /** Trabajo creado a partir del archivo (el backend ya lo validó). */
  readonly job = input<RucJob | null>(null);
  readonly robotState = input<RobotState>('checking');
  /** Se puede volver a empezar con otro archivo. */
  readonly canReset = input(false);

  readonly fileSelected = output<File>();
  readonly begin = output<void>();
  readonly newFlow = output<void>();

  protected onPicked(event: Event): void {
    const file = this.presenter.picked(event);
    if (file) this.fileSelected.emit(file);
  }
}

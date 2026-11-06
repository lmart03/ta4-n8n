import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FileUploadPresenter } from './file-upload.presenter';

/**
 * Zona para adjuntar un archivo: se puede arrastrar o elegir con el explorador (también con teclado).
 * Es genérica: no conoce el negocio; solo emite el archivo elegido y la pantalla decide qué hacer con él.
 */
@Component({
  selector: 'app-file-upload',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [FileUploadPresenter],
  templateUrl: './file-upload.component.html',
})
export class FileUploadComponent {
  protected readonly presenter = inject(FileUploadPresenter);

  /** Tipos aceptados, igual que el atributo `accept` de un input de archivo. */
  readonly accept = input('');
  readonly hint = input('');
  readonly disabled = input(false);

  readonly fileSelected = output<File>();

  protected onDrop(event: DragEvent): void {
    const file = this.presenter.onDrop(event, this.disabled());
    if (file) this.fileSelected.emit(file);
  }

  protected onPicked(event: Event): void {
    const file = this.presenter.onPicked(event);
    if (file) this.fileSelected.emit(file);
  }
}

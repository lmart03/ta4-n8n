import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ManualFallbackPresenter } from './manual-fallback.presenter';

/**
 * Plan B cuando SUNAT bloquea al robot: la persona consulta en SUNAT con su propio navegador y entrega aquí el
 * ZIP. El backend lo procesa igual que si lo hubiera traído el robot.
 */
@Component({
  selector: 'app-manual-fallback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ManualFallbackPresenter],
  templateUrl: './manual-fallback.component.html',
})
export class ManualFallbackComponent {
  protected readonly presenter = inject(ManualFallbackPresenter);

  readonly inputUrl = input<string | null>(null);
  readonly busy = input(false);
  /** Abierto por defecto (cuando el robot falló). */
  readonly open = input(false);

  readonly resultSelected = output<File>();
  /** El usuario pide consultar por la API de consultaderuc.pe. */
  readonly apiLookup = output<void>();

  protected onPicked(event: Event): void {
    const file = this.presenter.picked(event);
    if (file) this.resultSelected.emit(file);
  }
}

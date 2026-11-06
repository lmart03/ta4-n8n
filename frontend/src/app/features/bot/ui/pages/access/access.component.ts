import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { AccessPresenter } from './access.presenter';

/** Pantalla de acceso al panel del bot: una clave, sin usuarios. */
@Component({
  selector: 'app-access',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [AccessPresenter],
  templateUrl: './access.component.html',
})
export class AccessComponent {
  protected readonly presenter = inject(AccessPresenter);
}

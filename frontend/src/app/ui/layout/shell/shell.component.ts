import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ShellPresenter } from './shell.presenter';

/** Estructura global de la aplicación. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  providers: [ShellPresenter],
  templateUrl: './shell.component.html',
})
export class ShellComponent {
  protected readonly presenter = inject(ShellPresenter);
}

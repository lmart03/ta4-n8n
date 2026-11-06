import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AppPresenter } from './app.presenter';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  providers: [AppPresenter],
  templateUrl: './app.component.html',
})
export class AppComponent {
  protected readonly presenter = inject(AppPresenter);
}

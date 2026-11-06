import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { BotLayoutPresenter } from './bot-layout.presenter';

/** Área del bot: encabezado con la salud general, pestañas y el flujo en vivo abierto mientras se esté dentro. */
@Component({
  selector: 'app-bot-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  providers: [BotLayoutPresenter],
  templateUrl: './bot-layout.component.html',
})
export class BotLayoutComponent implements OnInit, OnDestroy {
  protected readonly presenter = inject(BotLayoutPresenter);

  ngOnInit(): void {
    this.presenter.open();
  }

  ngOnDestroy(): void {
    this.presenter.close();
  }
}

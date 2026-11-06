import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { BotTestsPresenter } from './bot-tests.presenter';

/** Pruebas del bot de punta a punta: la matriz PR-BOT del informe y una pregunta suelta. */
@Component({
  selector: 'app-bot-tests',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [BotTestsPresenter],
  templateUrl: './bot-tests.component.html',
})
export class BotTestsComponent implements OnInit {
  protected readonly presenter = inject(BotTestsPresenter);

  ngOnInit(): void {
    this.presenter.facade.loadTests();
  }
}

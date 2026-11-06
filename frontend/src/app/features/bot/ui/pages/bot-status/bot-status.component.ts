import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { BotLinkCardComponent } from '../../components/bot-link-card.component';
import { ConnectionCardComponent } from '../../components/connection-card.component';
import { BotStatusPresenter } from './bot-status.presenter';

/** Estado de cada pieza del bot y las dos acciones que reemplazan a los scripts: registrar el webhook y probar Gemini. */
@Component({
  selector: 'app-bot-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ConnectionCardComponent, BotLinkCardComponent],
  providers: [BotStatusPresenter],
  templateUrl: './bot-status.component.html',
})
export class BotStatusComponent {
  protected readonly presenter = inject(BotStatusPresenter);
}

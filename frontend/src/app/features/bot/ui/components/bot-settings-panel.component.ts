import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { BotSettings } from '../../domain/models/chat.model';
import { BotSettingsPanelPresenter } from './bot-settings-panel.presenter';

/** Ajustes de privacidad del panel: guardar o no las conversaciones y por cuánto tiempo. */
@Component({
  selector: 'app-bot-settings-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [BotSettingsPanelPresenter],
  templateUrl: './bot-settings-panel.component.html',
})
export class BotSettingsPanelComponent {
  protected readonly presenter = inject(BotSettingsPanelPresenter);

  readonly settings = input<BotSettings | null>(null);
  readonly save = output<BotSettings>();

  constructor() {
    effect(() => this.presenter.load(this.settings()));
  }

  submit(): void {
    const value = this.presenter.value();
    if (value !== null) this.save.emit(value);
  }
}

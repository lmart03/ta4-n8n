import { ChangeDetectionStrategy, Component, effect, inject, input, signal } from '@angular/core';
import { BotLinkCardPresenter } from './bot-link-card.presenter';

/** Enlace de Telegram del bot con su código QR: se escanea desde el celular para escribirle. */
@Component({
  selector: 'app-bot-link-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [BotLinkCardPresenter],
  templateUrl: './bot-link-card.component.html',
})
export class BotLinkCardComponent {
  protected readonly presenter = inject(BotLinkCardPresenter);

  readonly link = input<string | null>(null);
  readonly username = input<string | null>(null);

  protected readonly qr = signal<string | null>(null);

  constructor() {
    effect(() => {
      const link = this.link();
      if (link === null) {
        this.qr.set(null);
        return;
      }
      void this.presenter.qrFor(link).then((url) => this.qr.set(url));
    });
  }
}

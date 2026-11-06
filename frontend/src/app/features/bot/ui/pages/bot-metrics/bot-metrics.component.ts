import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { BotMetricsPresenter } from './bot-metrics.presenter';

/** Indicadores del bot sin contenido de conversaciones: volumen, latencia y reparto por fuente. */
@Component({
  selector: 'app-bot-metrics',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [BotMetricsPresenter],
  templateUrl: './bot-metrics.component.html',
})
export class BotMetricsComponent implements OnInit {
  protected readonly presenter = inject(BotMetricsPresenter);

  ngOnInit(): void {
    this.presenter.select(24);
  }
}

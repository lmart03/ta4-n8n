import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ComponentStatus } from '../../domain/models/bot-status.model';
import { ConnectionCardPresenter } from './connection-card.presenter';

/** Una pieza del bot (túnel, webhook, n8n, Gemini, datos) con su estado; el área de acciones entra por proyección. */
@Component({
  selector: 'app-connection-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ConnectionCardPresenter],
  templateUrl: './connection-card.component.html',
})
export class ConnectionCardComponent {
  protected readonly presenter = inject(ConnectionCardPresenter);

  readonly item = input.required<ComponentStatus>();
}

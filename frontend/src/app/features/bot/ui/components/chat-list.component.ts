import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { Chat } from '../../domain/models/chat.model';
import { ChatListPresenter } from './chat-list.presenter';

/** Lista de conversaciones: la más reciente primero, con no leídos, vista previa y quién atiende. */
@Component({
  selector: 'app-chat-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ChatListPresenter],
  templateUrl: './chat-list.component.html',
})
export class ChatListComponent {
  protected readonly presenter = inject(ChatListPresenter);

  readonly chats = input.required<Chat[]>();
  readonly selectedId = input<string | null>(null);
  readonly unread = input<Record<string, number>>({});
  readonly search = input('');
  readonly loaded = input(false);

  readonly chatSelected = output<string>();
  readonly searchChange = output<string>();
}

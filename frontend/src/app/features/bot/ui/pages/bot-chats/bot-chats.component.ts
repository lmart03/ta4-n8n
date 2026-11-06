import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { BotSettingsPanelComponent } from '../../components/bot-settings-panel.component';
import { ChatComposerComponent } from '../../components/chat-composer.component';
import { ChatListComponent } from '../../components/chat-list.component';
import { ChatThreadComponent } from '../../components/chat-thread.component';
import { BotChatsPresenter } from './bot-chats.presenter';

/** Conversaciones en vivo estilo WhatsApp: lista a la izquierda, chat a la derecha y respuesta como recepción. */
@Component({
  selector: 'app-bot-chats',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ChatListComponent, ChatThreadComponent, ChatComposerComponent, BotSettingsPanelComponent],
  providers: [BotChatsPresenter],
  templateUrl: './bot-chats.component.html',
})
export class BotChatsComponent implements OnInit {
  protected readonly presenter = inject(BotChatsPresenter);

  ngOnInit(): void {
    this.presenter.facade.loadChats();
  }
}

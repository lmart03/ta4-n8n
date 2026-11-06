import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, input, output, viewChild } from '@angular/core';
import { Chat, ChatMessage, ChatMode } from '../../domain/models/chat.model';
import { ChatThreadPresenter } from './chat-thread.presenter';

/** Conversación abierta: encabezado con quién atiende, burbujas agrupadas por día y fuente de cada respuesta. */
@Component({
  selector: 'app-chat-thread',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ChatThreadPresenter],
  templateUrl: './chat-thread.component.html',
})
export class ChatThreadComponent {
  protected readonly presenter = inject(ChatThreadPresenter);

  readonly chat = input.required<Chat>();
  readonly messages = input<ChatMessage[] | null>(null);

  readonly modeChange = output<ChatMode>();
  readonly deleteChat = output<void>();
  readonly back = output<void>();

  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');

  constructor() {
    // Al llegar un mensaje nuevo, baja al final solo si la persona ya estaba abajo (no le saltamos lo que está leyendo).
    effect(() => {
      this.messages();
      const element = this.scroller()?.nativeElement;
      if (element === undefined || !this.presenter.stickToBottom()) return;
      queueMicrotask(() => (element.scrollTop = element.scrollHeight));
    });
    effect(() => this.presenter.resetFor(this.chat().id));
  }
}

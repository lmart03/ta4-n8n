import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ChatMode } from '../../domain/models/chat.model';
import { ChatComposerPresenter } from './chat-composer.presenter';

/** Caja para responder como recepción. Avisa que al enviar se toma el chat y el bot deja de responder. */
@Component({
  selector: 'app-chat-composer',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ChatComposerPresenter],
  templateUrl: './chat-composer.component.html',
})
export class ChatComposerComponent {
  protected readonly presenter = inject(ChatComposerPresenter);

  readonly mode = input.required<ChatMode>();
  readonly busy = input(false);

  readonly send = output<string>();

  submit(): void {
    const text = this.presenter.take();
    if (text !== null) this.send.emit(text);
  }

  onKey(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      this.submit();
    }
  }
}

import { ChangeDetectionStrategy, Component, effect, inject, input, output } from '@angular/core';
import { AdminFaq, FaqForm } from '../../domain/models/faq.model';
import { ProblemDetails } from '../../domain/models/problem.model';
import { FaqFormPresenter } from './faq-form.presenter';

/** Formulario para crear o editar una pregunta frecuente. Valida antes de enviar y muestra el error del backend. */
@Component({
  selector: 'app-faq-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [FaqFormPresenter],
  templateUrl: './faq-form.component.html',
})
export class FaqFormComponent {
  protected readonly presenter = inject(FaqFormPresenter);

  /** `null` para una pregunta nueva. */
  readonly faq = input<AdminFaq | null>(null);
  readonly busy = input(false);
  readonly problem = input<ProblemDetails | null>(null);

  readonly save = output<FaqForm>();
  readonly dismiss = output<void>();

  constructor() {
    effect(() => this.presenter.load(this.faq()));
  }

  submit(): void {
    const form = this.presenter.validated();
    if (form !== null) this.save.emit(form);
  }
}

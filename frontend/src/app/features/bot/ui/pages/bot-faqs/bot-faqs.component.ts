import { ChangeDetectionStrategy, Component, OnInit, effect, inject } from '@angular/core';
import { FaqFormComponent } from '../../components/faq-form.component';
import { BotFaqsPresenter } from './bot-faqs.presenter';

/** Edición de las preguntas frecuentes y de los datos de la clínica: el bot los usa sin reprogramar nada. */
@Component({
  selector: 'app-bot-faqs',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FaqFormComponent],
  providers: [BotFaqsPresenter],
  templateUrl: './bot-faqs.component.html',
})
export class BotFaqsComponent implements OnInit {
  protected readonly presenter = inject(BotFaqsPresenter);

  constructor() {
    effect(() => this.presenter.loadBusiness(this.presenter.facade.business()));
  }

  ngOnInit(): void {
    this.presenter.facade.loadFaqs();
  }
}

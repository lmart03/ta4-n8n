import { Injectable, computed, signal } from '@angular/core';
import { AdminFaq, FAQ_CATEGORIES, FaqCategory, FaqForm } from '../../domain/models/faq.model';
import { CATEGORY_LABELS, EMPTY_FAQ_FORM, FAQ_LIMITS, hasErrors, validateFaqForm } from '../../domain/rules/faq-form.rule';

@Injectable()
export class FaqFormPresenter {
  readonly categories = FAQ_CATEGORIES.map((value) => ({ value, label: CATEGORY_LABELS[value] }));
  readonly limits = FAQ_LIMITS;

  readonly category = signal<FaqCategory>(EMPTY_FAQ_FORM.category);
  readonly question = signal('');
  readonly answer = signal('');
  readonly keywords = signal('');
  readonly active = signal(true);
  /** Los errores se muestran después del primer intento de guardar, no mientras la persona todavía escribe. */
  readonly submitted = signal(false);

  readonly form = computed<FaqForm>(() => ({
    category: this.category(),
    question: this.question(),
    answer: this.answer(),
    keywords: this.keywords(),
    active: this.active(),
  }));
  readonly errors = computed(() => (this.submitted() ? validateFaqForm(this.form()) : {}));

  load(faq: AdminFaq | null): void {
    this.category.set(faq?.category ?? EMPTY_FAQ_FORM.category);
    this.question.set(faq?.question ?? '');
    this.answer.set(faq?.answer ?? '');
    this.keywords.set(faq?.keywords ?? '');
    this.active.set(faq?.active ?? true);
    this.submitted.set(false);
  }

  /** El formulario listo para enviar, o `null` (y los errores a la vista) si algo falta. */
  validated(): FaqForm | null {
    this.submitted.set(true);
    return hasErrors(validateFaqForm(this.form())) ? null : this.form();
  }

  text(event: Event): string {
    return (event.target as HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement).value;
  }

  categoryOf(event: Event): FaqCategory {
    return (event.target as HTMLSelectElement).value as FaqCategory;
  }

  checked(event: Event): boolean {
    return (event.target as HTMLInputElement).checked;
  }
}

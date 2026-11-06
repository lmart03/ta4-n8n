import { Injectable, computed, inject, signal } from '@angular/core';
import { BotFacade } from '../../../application/services/bot.facade';
import { AdminFaq, BusinessInfo, FAQ_CATEGORIES } from '../../../domain/models/faq.model';
import { CATEGORY_LABELS } from '../../../domain/rules/faq-form.rule';

type BusinessField = keyof BusinessInfo;

const BUSINESS_FIELDS: { key: BusinessField; label: string; long?: boolean }[] = [
  { key: 'businessName', label: 'Nombre de la clínica' },
  { key: 'description', label: 'Descripción', long: true },
  { key: 'address', label: 'Dirección' },
  { key: 'district', label: 'Distrito' },
  { key: 'city', label: 'Ciudad' },
  { key: 'contactPhone', label: 'Teléfono de contacto (déjalo vacío si no hay)' },
  { key: 'appointmentInstructions', label: 'Cómo solicitar una cita', long: true },
];

@Injectable()
export class BotFaqsPresenter {
  readonly facade = inject(BotFacade);

  readonly categoryOptions = [{ value: 'ALL', label: 'Todas las categorías' }, ...FAQ_CATEGORIES.map((value) => ({ value, label: CATEGORY_LABELS[value] }))];
  readonly businessFields = BUSINESS_FIELDS;

  readonly search = signal('');
  readonly category = signal<string>('ALL');
  readonly confirmingDelete = signal<string | null>(null);
  readonly draft = signal<BusinessInfo | null>(null);

  readonly visible = computed(() => {
    const needle = this.fold(this.search());
    const category = this.category();
    return this.facade
      .faqs()
      .filter((faq) => (category === 'ALL' || faq.category === category) && (needle === '' || this.fold(`${faq.question} ${faq.answer}`).includes(needle)));
  });

  readonly editing = computed<AdminFaq | null>(() => {
    const id = this.facade.editingFaq();
    return id === null || id === 'new' ? null : (this.facade.faqs().find((faq) => faq.id === id) ?? null);
  });

  categoryLabel(category: string): string {
    return CATEGORY_LABELS[category as keyof typeof CATEGORY_LABELS] ?? category;
  }

  text(event: Event): string {
    return (event.target as HTMLInputElement | HTMLSelectElement).value;
  }

  loadBusiness(info: BusinessInfo | null): void {
    this.draft.set(info === null ? null : { ...info });
  }

  setBusinessField(key: BusinessField, value: string): void {
    const current = this.draft();
    if (current !== null) this.draft.set({ ...current, [key]: value });
  }

  businessValue(key: BusinessField): string {
    return this.draft()?.[key] ?? '';
  }

  saveBusiness(): void {
    const info = this.draft();
    if (info !== null) this.facade.saveBusiness({ ...info, contactPhone: info.contactPhone?.trim() ? info.contactPhone : null });
  }

  confirmDelete(faq: AdminFaq): void {
    this.confirmingDelete.set(faq.id);
  }

  delete(faq: AdminFaq): void {
    this.confirmingDelete.set(null);
    this.facade.deleteFaq(faq.id);
  }

  problemText(): string | null {
    const problem = this.facade.faqProblem();
    return problem === null ? null : (problem.detail ?? problem.title ?? 'No se pudo completar la operación.');
  }

  private fold(value: string): string {
    return value
      .normalize('NFD')
      .replace(/\p{M}/gu, '')
      .toLowerCase()
      .trim();
  }
}

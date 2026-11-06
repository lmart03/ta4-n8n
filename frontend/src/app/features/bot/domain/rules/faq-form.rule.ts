import { FaqForm } from '../models/faq.model';

export type FaqFormErrors = Partial<Record<'question' | 'answer' | 'keywords', string>>;

export const FAQ_LIMITS = { question: 500, answer: 1000, keywords: 500 } as const;

/** Valida el formulario antes de enviarlo (el backend repite las mismas reglas). */
export function validateFaqForm(form: FaqForm): FaqFormErrors {
  const errors: FaqFormErrors = {};
  if (form.question.trim() === '') errors.question = 'Escribe la pregunta.';
  else if (form.question.length > FAQ_LIMITS.question) errors.question = `Máximo ${FAQ_LIMITS.question} caracteres.`;
  if (form.answer.trim() === '') errors.answer = 'Escribe la respuesta que dará el bot.';
  else if (form.answer.length > FAQ_LIMITS.answer) errors.answer = `Máximo ${FAQ_LIMITS.answer} caracteres.`;
  if (form.keywords.length > FAQ_LIMITS.keywords) errors.keywords = `Máximo ${FAQ_LIMITS.keywords} caracteres.`;
  return errors;
}

export function hasErrors(errors: FaqFormErrors): boolean {
  return Object.keys(errors).length > 0;
}

export const EMPTY_FAQ_FORM: FaqForm = { category: 'SERVICIOS', question: '', answer: '', keywords: '', active: true };

export const CATEGORY_LABELS: Record<FaqForm['category'], string> = {
  HORARIOS: 'Horarios',
  UBICACION: 'Ubicación',
  SERVICIOS: 'Servicios',
  PRECIOS: 'Precios',
  CONTACTO: 'Contacto',
  CITAS: 'Citas',
};

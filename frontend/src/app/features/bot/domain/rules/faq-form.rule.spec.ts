import { EMPTY_FAQ_FORM, hasErrors, validateFaqForm } from './faq-form.rule';

describe('validateFaqForm', () => {
  it('pide pregunta y respuesta', () => {
    const errors = validateFaqForm(EMPTY_FAQ_FORM);
    expect(errors.question).toBeDefined();
    expect(errors.answer).toBeDefined();
    expect(hasErrors(errors)).toBe(true);
  });

  it('acepta un formulario completo con palabras clave vacías (las genera el backend)', () => {
    const errors = validateFaqForm({ ...EMPTY_FAQ_FORM, question: '¿Hay estacionamiento?', answer: 'Sí, frente a la clínica.' });
    expect(hasErrors(errors)).toBe(false);
  });

  it('respeta los máximos del backend', () => {
    const errors = validateFaqForm({ ...EMPTY_FAQ_FORM, question: 'a'.repeat(501), answer: 'b'.repeat(1001) });
    expect(errors.question).toContain('500');
    expect(errors.answer).toContain('1000');
  });
});

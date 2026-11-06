export const FAQ_CATEGORIES = ['HORARIOS', 'UBICACION', 'SERVICIOS', 'PRECIOS', 'CONTACTO', 'CITAS'] as const;
export type FaqCategory = (typeof FAQ_CATEGORIES)[number];

export interface AdminFaq {
  id: string;
  category: FaqCategory;
  question: string;
  answer: string;
  keywords: string;
  active: boolean;
  sortOrder: number;
  updatedAt: string | null;
}

/** Lo que se envía al crear o editar una pregunta (el backend genera las palabras clave si van vacías). */
export interface FaqForm {
  category: FaqCategory;
  question: string;
  answer: string;
  keywords: string;
  active: boolean;
}

export interface BusinessInfo {
  businessName: string;
  description: string;
  address: string;
  district: string;
  city: string;
  contactPhone: string | null;
  appointmentInstructions: string;
}

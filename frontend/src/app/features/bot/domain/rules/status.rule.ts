import { BotStatus, ComponentState, ComponentStatus } from '../models/bot-status.model';

export interface Health {
  state: ComponentState;
  title: string;
  detail: string;
}

/** Resume el estado de todas las piezas en una sola frase: lo peor que haya manda. */
export function overallHealth(status: BotStatus | null): Health {
  if (status === null) return { state: 'WARN', title: 'Revisando el bot…', detail: '' };
  const failing = status.components.filter((c) => c.state === 'ERROR');
  if (failing.length > 0) {
    return { state: 'ERROR', title: 'El bot no está listo', detail: `Falla: ${failing.map((c) => c.label).join(', ')}.` };
  }
  const warning = status.components.filter((c) => c.state === 'WARN');
  if (warning.length > 0) {
    return { state: 'WARN', title: 'El bot funciona con avisos', detail: `Revisa: ${warning.map((c) => c.label).join(', ')}.` };
  }
  return { state: 'OK', title: 'El bot está listo', detail: 'Todas las piezas responden.' };
}

/** Acción sugerida para una pieza que no está bien (el botón que la resuelve en el panel). */
export function suggestedAction(component: ComponentStatus): 'register-webhook' | 'ping-gemini' | null {
  if (component.key === 'webhook' && component.state !== 'OK') return 'register-webhook';
  if (component.key === 'gemini' && component.state === 'OK') return 'ping-gemini';
  return null;
}

export type ComponentState = 'OK' | 'WARN' | 'ERROR';

/** Estado de una pieza del bot: túnel, webhook, n8n, Gemini o datos. */
export interface ComponentStatus {
  key: 'tunnel' | 'webhook' | 'n8n' | 'gemini' | 'data';
  label: string;
  state: ComponentState;
  detail: string;
}

export interface BotStatus {
  components: ComponentStatus[];
  tunnelUrl: string | null;
  botUsername: string | null;
  botLink: string | null;
  autoRegister: boolean;
  checkedAt: string;
}

export interface WebhookResult {
  url: string;
  registered: boolean;
  detail: string;
}

export interface GeminiPing {
  ok: boolean;
  detail: string;
  latencyMs: number;
}

/** Resultado de una acción puntual (registrar webhook, probar Gemini) para mostrarlo junto al botón. */
export interface ActionResult {
  ok: boolean;
  text: string;
}

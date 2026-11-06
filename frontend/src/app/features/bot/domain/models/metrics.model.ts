export interface BotMetrics {
  hours: number;
  total: number;
  avgLatencyMs: number | null;
  p95LatencyMs: number | null;
  /** Cantidad de respuestas por fuente (FAQ, IA, CONTINGENCIA, DERIVADA, RECHAZO, AYUDA, HUMANO). */
  bySource: Record<string, number>;
  derivedPercent: number;
}

import { BotStatus, ComponentState } from '../models/bot-status.model';
import { overallHealth } from './status.rule';

function status(...states: ComponentState[]): BotStatus {
  const keys = ['tunnel', 'webhook', 'n8n', 'gemini', 'data'] as const;
  return {
    components: states.map((state, i) => ({ key: keys[i], label: `Pieza ${i}`, state, detail: '' })),
    tunnelUrl: null,
    botUsername: null,
    botLink: null,
    autoRegister: true,
    checkedAt: '2026-11-06T10:00:00Z',
  };
}

describe('overallHealth', () => {
  it('sin datos todavía dice que está revisando', () => {
    expect(overallHealth(null).title).toBe('Revisando el bot…');
  });

  it('todo en OK: el bot está listo', () => {
    expect(overallHealth(status('OK', 'OK', 'OK', 'OK', 'OK')).state).toBe('OK');
  });

  it('un aviso no bloquea pero se nombra', () => {
    const health = overallHealth(status('OK', 'OK', 'OK', 'WARN', 'OK'));
    expect(health.state).toBe('WARN');
    expect(health.detail).toContain('Pieza 3');
  });

  it('un error manda sobre los avisos', () => {
    const health = overallHealth(status('ERROR', 'WARN', 'OK', 'OK', 'OK'));
    expect(health.state).toBe('ERROR');
    expect(health.detail).toContain('Pieza 0');
    expect(health.detail).not.toContain('Pieza 1');
  });
});

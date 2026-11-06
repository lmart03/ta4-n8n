import { Injectable, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { BotFacade } from '../../../application/services/bot.facade';

@Injectable()
export class AccessPresenter {
  readonly facade = inject(BotFacade);
  private readonly route = inject(ActivatedRoute);

  readonly key = signal('');
  readonly visible = signal(false);

  onInput(event: Event): void {
    this.key.set((event.target as HTMLInputElement).value);
  }

  /** Solo se vuelve a rutas internas del panel: nunca a una dirección que venga de fuera. */
  private target(): string {
    const back = this.route.snapshot.queryParamMap.get('volver');
    return back !== null && back.startsWith('/bot') ? back : '/bot/estado';
  }

  submit(): void {
    this.facade.login(this.key(), this.target());
  }

  problemText(): string | null {
    const problem = this.facade.loginProblem();
    if (problem === null) return null;
    if (problem.status === 401) return 'La clave no es correcta. Revisa ADMIN_KEY en infra/.env.';
    if (problem.status === 429) return 'Demasiados intentos. Espera un minuto antes de volver a probar.';
    return problem.detail ?? problem.title ?? 'No se pudo validar la clave.';
  }
}

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AdminSession } from './admin-session.service';

/** Deja pasar solo con la clave de administrador puesta; si no, manda a la pantalla de acceso y recuerda a dónde iba. */
export const adminGuard: CanActivateFn = (_route, state) => {
  if (inject(AdminSession).authenticated()) return true;
  return inject(Router).createUrlTree(['/acceso'], { queryParams: { volver: state.url } });
};

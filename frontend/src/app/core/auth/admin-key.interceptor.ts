import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AdminSession } from './admin-session.service';

/** Rutas del backend que exigen la clave de administrador. */
const PROTECTED = /\/v1\/(bot|admin)\//;

/**
 * Añade `X-Admin-Key` a las rutas protegidas. Si el backend responde 401 con una sesión abierta, la clave dejó de
 * valer (cambió o venció): se cierra la sesión y se vuelve a la pantalla de acceso.
 */
export const adminKeyInterceptor: HttpInterceptorFn = (request, next) => {
  if (!PROTECTED.test(request.url)) return next(request);
  const session = inject(AdminSession);
  const router = inject(Router);
  const key = session.key();
  const signed = key ? request.clone({ setHeaders: { 'X-Admin-Key': key } }) : request;
  return next(signed).pipe(
    catchError((error: unknown) => {
      const isLogin = request.url.endsWith('/v1/bot/session');
      if (error instanceof HttpErrorResponse && error.status === 401 && key && !isLogin) {
        session.clear();
        void router.navigate(['/acceso']);
      }
      return throwError(() => error);
    }),
  );
};

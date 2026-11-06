import { AppEnvironment } from '../app/core/config/app-environment.model';

/** Producción: nginx sirve la app y redirige `/api` al backend (ver `deploy/nginx/default.conf`). */
export const environment: AppEnvironment = {
  production: true,
  apiUrl: '/api',
};

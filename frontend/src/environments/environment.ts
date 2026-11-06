import { AppEnvironment } from '../app/core/config/app-environment.model';

/**
 * Valores por defecto de desarrollo.
 *
 * En runtime pueden ser sobrescritos por `public/config.json`
 * (ver `src/app/core/config/runtime-config.loader.ts`).
 * En desarrollo, `ng serve` redirige `/api` al backend (ver `proxy.conf.json`).
 */
export const environment: AppEnvironment = {
  production: false,
  apiUrl: '/api',
};

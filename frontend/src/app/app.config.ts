import {
  ApplicationConfig,
  LOCALE_ID,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideStore } from '@ngrx/store';

import { routes } from './app.routes';
import { adminKeyInterceptor } from './core/auth/admin-key.interceptor';
import { loadRuntimeConfig } from './core/config/runtime-config.loader';

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'es-PE' },
    provideAppInitializer(loadRuntimeConfig),
    provideBrowserGlobalErrorListeners(),
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([adminKeyInterceptor])),
    // Store raíz; cada feature registra su parte del estado y sus efectos en su ruta.
    provideStore(),
    provideEffects(),
  ],
};

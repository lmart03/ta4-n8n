import { Routes } from '@angular/router';
import { provideEffects } from '@ngrx/effects';
import { provideState } from '@ngrx/store';
import { adminGuard } from './core/auth/admin.guard';
import { BotEffects, botFeature } from './features/bot/application/store';
import { RucFlowEffects, rucFlowFeature } from './features/ruc-jobs/application/store';
import { ShellComponent } from './ui/layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'ruc' },
      {
        path: 'ruc',
        title: 'Consulta múltiple de RUC',
        providers: [provideState(rucFlowFeature), provideEffects(RucFlowEffects)],
        loadComponent: () =>
          import('./features/ruc-jobs/ui/pages/ruc-flow/ruc-flow.component').then((m) => m.RucFlowComponent),
      },
      {
        // El estado y los efectos del bot viven en este nivel para que sobrevivan al pasar del acceso al panel.
        path: '',
        providers: [provideState(botFeature), provideEffects(BotEffects)],
        children: [
          {
            path: 'acceso',
            title: 'Acceso al panel del bot',
            loadComponent: () => import('./features/bot/ui/pages/access/access.component').then((m) => m.AccessComponent),
          },
          {
            path: 'bot',
            canActivate: [adminGuard],
            loadComponent: () =>
              import('./features/bot/ui/pages/bot-layout/bot-layout.component').then((m) => m.BotLayoutComponent),
            children: [
              { path: '', pathMatch: 'full', redirectTo: 'estado' },
              {
                path: 'estado',
                title: 'Estado del bot',
                loadComponent: () =>
                  import('./features/bot/ui/pages/bot-status/bot-status.component').then((m) => m.BotStatusComponent),
              },
              {
                path: 'conversaciones',
                title: 'Conversaciones del bot',
                loadComponent: () =>
                  import('./features/bot/ui/pages/bot-chats/bot-chats.component').then((m) => m.BotChatsComponent),
              },
              {
                path: 'pruebas',
                title: 'Pruebas del bot',
                loadComponent: () =>
                  import('./features/bot/ui/pages/bot-tests/bot-tests.component').then((m) => m.BotTestsComponent),
              },
              {
                path: 'faqs',
                title: 'Preguntas frecuentes del bot',
                loadComponent: () =>
                  import('./features/bot/ui/pages/bot-faqs/bot-faqs.component').then((m) => m.BotFaqsComponent),
              },
              {
                path: 'metricas',
                title: 'Métricas del bot',
                loadComponent: () =>
                  import('./features/bot/ui/pages/bot-metrics/bot-metrics.component').then((m) => m.BotMetricsComponent),
              },
            ],
          },
        ],
      },
    ],
  },
  { path: '**', redirectTo: '' },
];

import { Injectable } from '@angular/core';

export type ModuleIcon = 'ruc' | 'bot';

export interface ModuleLink {
  path: string;
  label: string;
  /** Texto corto para pantallas angostas, donde el selector comparte el ancho con la marca. */
  shortLabel: string;
  icon: ModuleIcon;
}

/** Estructura global: marca del producto, selector de módulos (nivel 1) y zona de contenido. */
@Injectable()
export class ShellPresenter {
  readonly mainId = 'main-content';
  readonly product = 'TA4';
  readonly tagline = 'Plataforma de automatización';

  readonly modules: ModuleLink[] = [
    { path: '/ruc', label: 'Consulta RUC', shortLabel: 'RUC', icon: 'ruc' },
    { path: '/bot', label: 'Bot de Telegram', shortLabel: 'Bot', icon: 'bot' },
  ];
}

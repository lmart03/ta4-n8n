import { enableProdMode } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { bootstrapApplication } from '@angular/platform-browser';

import { AppComponent } from './app/app.component';
import { appConfig } from './app/app.config';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}
registerLocaleData(localeEsPe);

bootstrapApplication(AppComponent, appConfig).catch((err: unknown) => console.error(err)); // NOSONAR: Top-level await not supported by current build target

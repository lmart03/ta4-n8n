import { Injectable, signal } from '@angular/core';

@Injectable()
export class BotLinkCardPresenter {
  readonly copied = signal(false);

  /** QR del enlace como imagen `data:` (la librería se carga solo cuando hace falta). */
  async qrFor(link: string): Promise<string | null> {
    try {
      const module = await import('qrcode');
      // La librería es CommonJS: según el empaquetador la API llega en `default` o en el propio módulo.
      const qrcode = (module.default as typeof module | undefined) ?? module;
      return await qrcode.toDataURL(link, { margin: 1, width: 192, color: { dark: '#0f172a', light: '#ffffff' } });
    } catch {
      return null;
    }
  }

  async copy(link: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(link);
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    } catch {
      this.copied.set(false);
    }
  }
}

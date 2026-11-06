import { Injectable, computed, signal } from '@angular/core';

const STORAGE_KEY = 'ta4.adminKey';

/**
 * Clave de administrador del panel del bot. Vive solo en `sessionStorage` (se borra al cerrar la pestaña) y nunca
 * se guarda en el Store ni se muestra. Cada petición a `/v1/bot` y `/v1/admin` la envía el interceptor.
 */
@Injectable({ providedIn: 'root' })
export class AdminSession {
  private readonly current = signal<string | null>(read());

  readonly key = this.current.asReadonly();
  readonly authenticated = computed(() => this.current() !== null);

  set(key: string): void {
    this.current.set(key);
    try {
      sessionStorage.setItem(STORAGE_KEY, key);
    } catch {
      // Sin almacenamiento (ventana privada): la clave dura mientras la página siga abierta.
    }
  }

  clear(): void {
    this.current.set(null);
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      // Nada que limpiar.
    }
  }
}

function read(): string | null {
  try {
    return sessionStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

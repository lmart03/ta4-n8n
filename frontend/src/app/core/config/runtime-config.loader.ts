import { environment } from 'src/environments/environment';
import { AppEnvironment } from './app-environment.model';

/** Ruta pública del archivo de configuración (respeta el `<base href>`). */
const CONFIG_FILE = 'config.json';

/** Tiempo máximo de espera de la descarga antes de usar los valores por defecto. */
const REQUEST_TIMEOUT_MS = 5000;

type UnknownRecord = Record<string, unknown>;

function isRecord(value: unknown): value is UnknownRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function readNonEmptyString(source: UnknownRecord, key: string): string | undefined {
  const value = source[key];
  if (typeof value !== 'string') return undefined;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : undefined;
}

function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '');
}

/**
 * Aplica de forma segura (validando tipos y claves) el contenido de
 * `config.json` sobre el objeto `environment`.
 */
export function applyRuntimeConfig(target: AppEnvironment, raw: unknown): void {
  if (!isRecord(raw)) return;

  const apiUrl = readNonEmptyString(raw, 'apiUrl');
  if (apiUrl) target.apiUrl = stripTrailingSlash(apiUrl);
}

/**
 * Descarga `config.json` y lo aplica sobre `environment`. Nunca lanza: ante
 * cualquier error se conservan los valores compilados por defecto.
 *
 * Se registra como `provideAppInitializer` para garantizar que la configuración
 * esté lista antes de instanciar cualquier servicio que lea `environment`.
 */
export async function loadRuntimeConfig(): Promise<void> {
  const url = new URL(CONFIG_FILE, document.baseURI).toString();
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(url, { cache: 'no-store', signal: controller.signal });
    if (!response.ok) {
      console.warn(`[RuntimeConfig] ${CONFIG_FILE} no disponible (HTTP ${response.status}).`);
      return;
    }
    const raw: unknown = await response.json();
    applyRuntimeConfig(environment, raw);
  } catch (error) {
    console.warn(`[RuntimeConfig] No se pudo cargar ${CONFIG_FILE}.`, error);
  } finally {
    clearTimeout(timeout);
  }
}

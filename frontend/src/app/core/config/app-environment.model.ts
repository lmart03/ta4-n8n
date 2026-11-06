/** Configuración de la aplicación. Los valores pueden sobrescribirse en runtime con `config.json`. */
export interface AppEnvironment {
  production: boolean;
  /** Base de la API (sin barra final). Por defecto `/api`, resuelta por el proxy de dev o por nginx. */
  apiUrl: string;
}

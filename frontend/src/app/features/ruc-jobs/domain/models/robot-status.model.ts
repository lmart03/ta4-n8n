/** Respuesta de `GET /v1/ruc/robot/status`: si el lanzador local del robot está escuchando. */
export interface RobotStatus {
  online: boolean;
  lastSeenAt: string | null;
}

/** Lo que la pantalla muestra del robot: `checking` es el instante antes de la primera respuesta. */
export type RobotState = 'checking' | 'online' | 'offline' | 'unreachable';

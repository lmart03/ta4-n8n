package pe.edu.ta4.ruc.service;

import pe.edu.ta4.ruc.dto.RobotStatusResponse;

/**
 * Presencia del lanzador local del robot. El lanzador consulta la cola cada pocos segundos; esa consulta es su
 * latido, y si deja de llegar el robot se considera desconectado. Vive en memoria: tras reiniciar el backend el
 * estado vuelve a "desconectado" hasta el siguiente latido (unos 2 segundos).
 */
public interface RobotPresenceService {
    /** El lanzador acaba de consultar la cola. */
    void heartbeat();

    RobotStatusResponse status();
}

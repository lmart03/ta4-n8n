package pe.edu.ta4.ruc.service;

import java.util.List;
import java.util.UUID;
import pe.edu.ta4.ruc.dto.FileDownload;
import pe.edu.ta4.ruc.dto.RucJobEventResponse;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.dto.RucJobResultsResponse;
import pe.edu.ta4.ruc.model.JobStatus;

/** Casos de uso de la consulta múltiple de RUC: del Excel cargado al Excel de resultados. */
public interface RucJobService {

    /** Valida el Excel, crea el trabajo y deja listo el TXT para el robot. 422 si el Excel es inválido. */
    RucJobResponse create(String filename, byte[] workbook);

    List<RucJobResponse> list(int limit);

    RucJobResponse get(UUID id);

    List<RucJobEventResponse> events(UUID id);

    /** TXT que el robot sube a la Consulta Múltiple de SUNAT. */
    FileDownload input(UUID id);

    /**
     * Pide arrancar el robot (lo llama la web con "Empezar flujo"). Solo si el trabajo está listo o falló (reintento);
     * 409 si el robot ya está en curso o el trabajo terminó. Pedirlo dos veces seguidas no duplica nada.
     */
    RucJobResponse requestStart(UUID id);

    /** Cola del robot: trabajos con arranque pedido y aún no tomado, del más antiguo al más reciente. */
    List<RucJobResponse> pendingStarts();

    /** El lanzador local toma el pedido de arranque; 409 si ya lo tomó otro o no hay pedido. */
    RucJobResponse claimStart(UUID id);

    /** El robot informa su avance (EN_CONSULTA, REQUIERE_INTERVENCION o FALLIDO). */
    RucJobResponse reportStatus(UUID id, JobStatus status, String message);

    /**
     * Recibe el ZIP de SUNAT, lo procesa y completa el trabajo. Solo se acepta una vez (409 si ya se
     * entregó); un ZIP inválido da 422 y deja el trabajo como estaba, con el rechazo en su historial.
     */
    RucJobResponse receiveResult(UUID id, byte[] zip);

    /**
     * Plan C cuando SUNAT bloquea al robot: consulta los RUC en una API de terceros y completa el trabajo. El
     * trabajo queda marcado con esa fuente. 503 si la API no está configurada o no responde (el trabajo no cambia).
     */
    RucJobResponse lookupViaApi(UUID id);

    RucJobResultsResponse results(UUID id);

    /** Excel de resultados; 409 mientras el trabajo no esté completado. */
    FileDownload export(UUID id);

    /** ZIP original tal como lo entregó el robot. */
    FileDownload resultFile(UUID id);
}

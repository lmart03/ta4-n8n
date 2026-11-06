package pe.edu.ta4.ruc.dto;

import java.util.List;
import java.util.UUID;
import pe.edu.ta4.ruc.model.JobStatus;

/** Resultados normalizados de un trabajo: una fila por RUC solicitado, en el orden del Excel. */
public record RucJobResultsResponse(UUID jobId, JobStatus status, List<RucJobItemResponse> items) {
}

package pe.edu.ta4.ruc.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.model.RucJob;

/**
 * @param startRequestedAt cuándo se pidió arrancar el robot desde la web; nulo si no hay un arranque pendiente
 *                         (la pantalla lo usa para avisar si el lanzador local no está abierto)
 */
public record RucJobResponse(UUID id, JobStatus status, int total, int foundCount, int missingCount,
                             int unexpectedCount, String originalFilename, String summary,
                             OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime completedAt,
                             OffsetDateTime startRequestedAt) {

    public static RucJobResponse from(RucJob job) {
        return new RucJobResponse(job.getId(), job.getStatus(), job.getTotal(), job.getFoundCount(),
            job.getMissingCount(), job.getUnexpectedCount(), job.getOriginalFilename(), job.getSummary(),
            job.getCreatedAt(), job.getUpdatedAt(), job.getCompletedAt(), job.getStartRequestedAt());
    }
}

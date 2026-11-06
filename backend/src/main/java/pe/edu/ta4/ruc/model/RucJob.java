package pe.edu.ta4.ruc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Un trabajo de consulta múltiple: nace al cargar el Excel y termina cuando se procesa el resultado. */
@Entity
@Table(name = "ruc_job")
public class RucJob {
    @Id
    private UUID id;
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private int total;
    @Column(name = "original_filename")
    private String originalFilename;
    @Column(name = "found_count")
    private int foundCount;
    @Column(name = "missing_count")
    private int missingCount;
    @Column(name = "unexpected_count")
    private int unexpectedCount;
    private String summary;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
    @Column(name = "completed_at")
    private OffsetDateTime completedAt;
    @Column(name = "start_requested_at")
    private OffsetDateTime startRequestedAt;

    protected RucJob() {
        // requerido por JPA
    }

    public RucJob(UUID id, int total, String originalFilename, OffsetDateTime now) {
        this.id = id;
        this.status = JobStatus.RECIBIDO;
        this.total = total;
        this.originalFilename = originalFilename;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Cambia de estado respetando las transiciones permitidas. */
    public void moveTo(JobStatus next, OffsetDateTime now) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Transición no permitida: " + status + " -> " + next);
        }
        this.status = next;
        this.updatedAt = now;
        if (next.isFinal()) this.completedAt = now;
        // Una vez que el robot corre (o terminó), ya no hay un arranque pendiente que recoger.
        if (next != JobStatus.FALLIDO && next != JobStatus.LISTO_PARA_ROBOT) this.startRequestedAt = null;
    }

    /** La web pidió arrancar el robot; queda en la cola hasta que el lanzador lo tome. */
    public void requestStart(OffsetDateTime now) {
        this.startRequestedAt = now;
        this.updatedAt = now;
    }

    /** El lanzador tomó el pedido: sale de la cola. */
    public void claimStart(OffsetDateTime now) {
        this.startRequestedAt = null;
        this.updatedAt = now;
    }

    /** Guarda el resultado del cotejo contra SUNAT. */
    public void applyOutcome(int found, int missing, int unexpected, String summary) {
        this.foundCount = found;
        this.missingCount = missing;
        this.unexpectedCount = unexpected;
        this.summary = summary;
    }

    public UUID getId() { return id; }
    public JobStatus getStatus() { return status; }
    public int getTotal() { return total; }
    public String getOriginalFilename() { return originalFilename; }
    public int getFoundCount() { return foundCount; }
    public int getMissingCount() { return missingCount; }
    public int getUnexpectedCount() { return unexpectedCount; }
    public String getSummary() { return summary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getStartRequestedAt() { return startRequestedAt; }
}

package pe.edu.ta4.ruc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Entrada del historial de un trabajo: cambios de estado y rechazos (para trazabilidad). */
@Entity
@Table(name = "ruc_job_event")
public class RucJobEvent {
    @Id
    private UUID id;
    @Column(name = "job_id")
    private UUID jobId;
    @Column(name = "seq_no")
    private int sequence;
    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private JobStatus fromStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status")
    private JobStatus toStatus;
    private String message;

    protected RucJobEvent() {
        // requerido por JPA
    }

    public RucJobEvent(UUID id, UUID jobId, int sequence, OffsetDateTime occurredAt,
                       JobStatus fromStatus, JobStatus toStatus, String message) {
        this.id = id;
        this.jobId = jobId;
        this.sequence = sequence;
        this.occurredAt = occurredAt;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.message = message;
    }

    public UUID getId() { return id; }
    public UUID getJobId() { return jobId; }
    public int getSequence() { return sequence; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public JobStatus getFromStatus() { return fromStatus; }
    public JobStatus getToStatus() { return toStatus; }
    public String getMessage() { return message; }
}

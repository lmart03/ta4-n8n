package pe.edu.ta4.ruc.dto;

import java.time.OffsetDateTime;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.model.RucJobEvent;

public record RucJobEventResponse(int sequence, OffsetDateTime occurredAt, JobStatus from, JobStatus to, String message) {

    public static RucJobEventResponse from(RucJobEvent event) {
        return new RucJobEventResponse(event.getSequence(), event.getOccurredAt(), event.getFromStatus(),
            event.getToStatus(), event.getMessage());
    }
}

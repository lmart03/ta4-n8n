package pe.edu.ta4.ruc.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.ta4.ruc.model.JobStatus;

/** Estado que reporta el robot (EN_CONSULTA, REQUIERE_INTERVENCION o FALLIDO) con un mensaje opcional. */
public record StatusUpdateRequest(
    @NotNull(message = "El estado es obligatorio") JobStatus status,
    @Size(max = 500, message = "El mensaje admite hasta 500 caracteres") String message) {
}

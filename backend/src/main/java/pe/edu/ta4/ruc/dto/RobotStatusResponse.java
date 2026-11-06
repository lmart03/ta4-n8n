package pe.edu.ta4.ruc.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/** Si el lanzador local del robot está escuchando la cola (la web lo muestra como "Robot listo"). */
@Schema(description = "Estado del lanzador local del robot")
public record RobotStatusResponse(
    @Schema(description = "El lanzador consultó la cola hace pocos segundos") boolean online,
    @Schema(description = "Última vez que el lanzador consultó la cola; null si aún no lo hizo desde que arrancó el backend")
    OffsetDateTime lastSeenAt) {
}

package pe.edu.ta4.ruc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ta4.ruc.dto.RobotStatusResponse;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.service.RobotPresenceService;
import pe.edu.ta4.ruc.service.RucJobService;

/**
 * Cola del robot. El navegador no puede lanzar UiPath y el backend corre en Docker, así que el lanzador local
 * (en Windows) consulta esta cola cada pocos segundos: no hay ningún puerto abierto en el equipo.
 */
@RestController
@RequestMapping("/api/v1/ruc/robot")
@Tag(name = "Cola del robot", description = "Arranques pedidos desde la web que recoge el lanzador local.")
public class RucRobotQueueController {
    private final RucJobService service;
    private final RobotPresenceService presence;

    public RucRobotQueueController(RucJobService service, RobotPresenceService presence) {
        this.service = service;
        this.presence = presence;
    }

    @GetMapping("/pending-start")
    @Operation(summary = "Trabajos con arranque pedido y aún no tomado",
        description = "Del más antiguo al más reciente. Cada consulta cuenta como latido del lanzador (ver /status).")
    public ResponseEntity<List<RucJobResponse>> pending() {
        presence.heartbeat();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.pendingStarts());
    }

    @GetMapping("/status")
    @Operation(summary = "¿Está el lanzador del robot escuchando?",
        description = "online=true si el lanzador consultó la cola en los últimos 8 segundos.")
    public ResponseEntity<RobotStatusResponse> status() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(presence.status());
    }

    @PostMapping("/jobs/{id}/claim")
    @Operation(summary = "Tomar el pedido de arranque de un trabajo",
        description = "El lanzador lo llama justo antes de ejecutar el robot; así dos lanzadores no ejecutan el mismo trabajo.")
    @ApiResponse(responseCode = "409", description = "No hay un arranque pendiente (application/problem+json)")
    public RucJobResponse claim(@PathVariable UUID id) {
        return service.claimStart(id);
    }
}

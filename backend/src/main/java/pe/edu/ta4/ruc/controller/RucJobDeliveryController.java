package pe.edu.ta4.ruc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.dto.StatusUpdateRequest;
import pe.edu.ta4.ruc.service.RucJobService;

/**
 * Entrega de datos entre el backend y el robot de UiPath: descargar el TXT, reportar avance y devolver el ZIP
 * de SUNAT. La entrega del ZIP también la puede hacer la pantalla web como respaldo manual.
 */
@RestController
@RequestMapping("/api/v1/ruc/jobs/{id}")
@Tag(name = "Robot UiPath", description = "Intercambio de archivos y estados con el robot atendido.")
public class RucJobDeliveryController {
    private final RucJobService service;

    public RucJobDeliveryController(RucJobService service) {
        this.service = service;
    }

    @org.springframework.web.bind.annotation.GetMapping("/input")
    @Operation(summary = "Descargar el TXT para la Consulta Múltiple de SUNAT",
        description = "Un RUC por línea terminado en | (UTF-8, CRLF).")
    public ResponseEntity<byte[]> input(@PathVariable UUID id) {
        return DownloadResponses.attachment(service.input(id));
    }

    @PostMapping(value = "/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "El robot reporta su avance",
        description = "Estados permitidos: EN_CONSULTA, REQUIERE_INTERVENCION y FALLIDO. Repetir el mismo estado no hace nada.")
    @ApiResponse(responseCode = "409", description = "Transición no permitida desde el estado actual (application/problem+json)")
    public RucJobResponse status(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        return service.reportStatus(id, request.status(), request.message());
    }

    @PostMapping(value = "/result", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Entregar el ZIP de resultados de SUNAT",
        description = "Se acepta una sola vez por trabajo. ZIP inválido: 422 y el trabajo queda como estaba.")
    @ApiResponse(responseCode = "409", description = "El resultado ya fue entregado o el trabajo aún no puede recibirlo (application/problem+json)")
    @ApiResponse(responseCode = "422", description = "ZIP inválido o inseguro (application/problem+json)")
    public RucJobResponse result(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return service.receiveResult(id, RucJobController.bytes(file));
    }
}

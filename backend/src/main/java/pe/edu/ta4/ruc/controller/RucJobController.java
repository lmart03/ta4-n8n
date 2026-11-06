package pe.edu.ta4.ruc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.ta4.ruc.dto.RucJobEventResponse;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.dto.RucJobResultsResponse;
import pe.edu.ta4.ruc.service.RucJobService;
import pe.edu.ta4.shared.error.ApiException;

/** API del usuario: cargar el Excel de RUC, seguir el trabajo y descargar resultados. */
@RestController
@RequestMapping("/api/v1/ruc/jobs")
@Tag(name = "Trabajos RUC", description = "Consulta múltiple de RUC: del Excel de entrada al Excel de resultados.")
public class RucJobController {
    private final RucJobService service;

    public RucJobController(RucJobService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar el Excel de RUC y crear un trabajo",
        description = "Hoja RUC (o la primera) con una columna ruc: 1 a 20 RUC válidos y sin duplicados. "
            + "Si el Excel es inválido responde 422 con todos los problemas y no crea nada.")
    @ApiResponse(responseCode = "201", description = "Trabajo creado; Location apunta a su detalle")
    @ApiResponse(responseCode = "413", description = "Archivo demasiado grande (application/problem+json)")
    @ApiResponse(responseCode = "422", description = "Excel inválido (application/problem+json con errors[])")
    public ResponseEntity<RucJobResponse> create(@RequestParam("file") MultipartFile file) {
        RucJobResponse job = service.create(file.getOriginalFilename(), bytes(file));
        return ResponseEntity.created(URI.create("/api/v1/ruc/jobs/" + job.id())).body(job);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Empezar el flujo: pedir que arranque el robot",
        description = "Deja el pedido en la cola del robot; el lanzador local lo toma en unos segundos y el robot "
            + "informa su avance por /status. Solo con el trabajo en LISTO_PARA_ROBOT o FALLIDO (reintento).")
    @ApiResponse(responseCode = "202", description = "Arranque solicitado (o ya estaba solicitado)")
    @ApiResponse(responseCode = "409", description = "El robot ya está en curso o el trabajo terminó (application/problem+json)")
    public ResponseEntity<RucJobResponse> start(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(service.requestStart(id));
    }

    @PostMapping("/{id}/lookup-api")
    @Operation(summary = "Consultar los RUC por API (plan C, sin robot)",
        description = "Cuando SUNAT bloquea al robot: consulta cada RUC en la API de consultaderuc.pe y completa el "
            + "trabajo. El trabajo queda marcado con esa fuente (historial y resumen); no es la Consulta Múltiple de SUNAT. "
            + "Requiere el token CONSULTA_RUC_API_TOKEN.")
    @ApiResponse(responseCode = "200", description = "Trabajo completado con los datos de la API")
    @ApiResponse(responseCode = "409", description = "El trabajo ya terminó o no puede recibir resultados todavía")
    @ApiResponse(responseCode = "503", description = "API sin configurar, sin cuota o sin conexión (application/problem+json)")
    public RucJobResponse lookupViaApi(@PathVariable UUID id) {
        return service.lookupViaApi(id);
    }

    @GetMapping
    @Operation(summary = "Listar los últimos trabajos", description = "Del más reciente al más antiguo; máximo 100.")
    public List<RucJobResponse> list(@RequestParam(defaultValue = "20") int limit) {
        return service.list(limit);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Estado, totales y observaciones de un trabajo")
    @ApiResponse(responseCode = "404", description = "El trabajo no existe (application/problem+json)")
    public RucJobResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Historial de estados y rechazos del trabajo")
    public List<RucJobEventResponse> events(@PathVariable UUID id) {
        return service.events(id);
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "Resultados normalizados (una fila por RUC, en el orden del Excel)")
    public ResponseEntity<RucJobResultsResponse> results(@PathVariable UUID id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.results(id));
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Descargar el Excel de resultados",
        description = "Solo cuando el trabajo está COMPLETADO o COMPLETADO_CON_OBSERVACIONES; si no, 409.")
    @ApiResponse(responseCode = "409", description = "El trabajo aún no tiene resultados (application/problem+json)")
    public ResponseEntity<byte[]> export(@PathVariable UUID id) {
        return DownloadResponses.attachment(service.export(id));
    }

    @GetMapping("/{id}/result-file")
    @Operation(summary = "Descargar el ZIP original de SUNAT", description = "Se conserva sin modificar.")
    public ResponseEntity<byte[]> resultFile(@PathVariable UUID id) {
        return DownloadResponses.attachment(service.resultFile(id));
    }

    static byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.unavailable("No se pudo leer el archivo recibido.");
        }
    }
}

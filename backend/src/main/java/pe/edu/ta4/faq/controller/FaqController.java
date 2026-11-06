package pe.edu.ta4.faq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ta4.faq.dto.FaqResponse;
import pe.edu.ta4.faq.dto.FaqSearchResponse;
import pe.edu.ta4.faq.service.FaqService;

@RestController
@RequestMapping("/api/v1/faqs")
@Tag(name = "Preguntas frecuentes")
public class FaqController {
    private final FaqService service;

    public FaqController(FaqService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar FAQ", description = "Por defecto devuelve solo las activas, ordenadas por sortOrder.")
    public List<FaqResponse> list(@RequestParam(defaultValue = "true") boolean active) {
        return service.list(active);
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar contexto FAQ",
        description = "Coincidencias léxicas normalizadas, no semánticas. q tiene máximo 500 caracteres. Sin coincidencias devuelve lista vacía.")
    @ApiResponse(responseCode = "400", description = "Consulta vacía, inválida o demasiado larga (application/problem+json)")
    @ApiResponse(responseCode = "503", description = "Datos temporalmente no disponibles (application/problem+json)")
    public FaqSearchResponse search(@RequestParam(required = false) String q) {
        return service.search(q);
    }
}

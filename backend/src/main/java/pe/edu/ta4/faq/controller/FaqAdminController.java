package pe.edu.ta4.faq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ta4.faq.dto.FaqAdminRequest;
import pe.edu.ta4.faq.dto.FaqAdminResponse;
import pe.edu.ta4.faq.service.FaqAdminService;

/** Edición de FAQ desde el panel. Exige el header {@code X-Admin-Key}. */
@RestController
@RequestMapping("/api/v1/admin/faqs")
@Tag(name = "Preguntas frecuentes")
public class FaqAdminController {
    private final FaqAdminService service;

    public FaqAdminController(FaqAdminService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas las FAQ (activas e inactivas)")
    public List<FaqAdminResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una FAQ")
    public FaqAdminResponse create(@Valid @RequestBody FaqAdminRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar una FAQ")
    public FaqAdminResponse update(@PathVariable UUID id, @Valid @RequestBody FaqAdminRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Borrar una FAQ")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}

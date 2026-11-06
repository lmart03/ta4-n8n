package pe.edu.ta4.business.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ta4.business.dto.BusinessAdminRequest;
import pe.edu.ta4.business.dto.BusinessResponse;
import pe.edu.ta4.business.service.BusinessAdminService;

/** Edición de los datos de la clínica. Exige el header {@code X-Admin-Key}. */
@RestController
@RequestMapping("/api/v1/admin/business")
@Tag(name = "Clínica")
public class BusinessAdminController {
    private final BusinessAdminService service;

    public BusinessAdminController(BusinessAdminService service) {
        this.service = service;
    }

    @PutMapping
    @Operation(summary = "Actualizar los datos de la clínica")
    public BusinessResponse update(@Valid @RequestBody BusinessAdminRequest request) {
        return service.update(request);
    }
}

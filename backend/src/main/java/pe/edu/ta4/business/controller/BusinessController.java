package pe.edu.ta4.business.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ta4.business.dto.BusinessResponse;
import pe.edu.ta4.business.service.BusinessService;

@RestController
@RequestMapping("/api/v1/business")
@Tag(name = "Clínica")
public class BusinessController {
    private final BusinessService service;

    public BusinessController(BusinessService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Datos generales de la Clínica Soraka (ficticia)")
    public BusinessResponse get() {
        return service.getBusiness();
    }
}

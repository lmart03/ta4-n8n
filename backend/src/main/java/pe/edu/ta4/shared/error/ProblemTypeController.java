package pe.edu.ta4.shared.error;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Documentación navegable de los tipos de problema: es lo que apunta {@code type} en cada error. */
@RestController
@RequestMapping("/problems")
@Tag(name = "Problemas", description = "Catálogo de tipos de error (RFC 9457).")
public class ProblemTypeController {

    @GetMapping
    @Operation(summary = "Listar los tipos de problema")
    public List<ProblemTypeView> list() {
        return Arrays.stream(ProblemType.values()).map(ProblemTypeView::from).toList();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Describir un tipo de problema")
    public ProblemTypeView get(@PathVariable String slug) {
        return ProblemType.bySlug(slug).map(ProblemTypeView::from)
            .orElseThrow(() -> ApiException.notFound("No existe el tipo de problema '" + slug + "'."));
    }

    public record ProblemTypeView(String type, String title, int status, String description) {
        static ProblemTypeView from(ProblemType problem) {
            return new ProblemTypeView(problem.uri().toString(), problem.title(), problem.status(), problem.description());
        }
    }
}

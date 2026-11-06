package pe.edu.ta4.shared;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.edu.ta4.shared.error.GlobalExceptionHandler;
import pe.edu.ta4.shared.error.ProblemTypeController;

/** Errores que no vienen de la lógica de negocio: caídas, límites y rutas inexistentes. */
class GlobalExceptionHandlerTest {
    private static final String PROBLEM_JSON = "application/problem+json";

    @RestController
    static class Boom {
        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("clave secreta: abc123");
        }

        @GetMapping("/db")
        String db() {
            throw new DataAccessResourceFailureException("jdbc:postgresql://host/ta4 password=secreto");
        }

        @GetMapping("/big")
        String big() {
            throw new MaxUploadSizeExceededException(2_000_000);
        }
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new Boom(), new ProblemTypeController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void anUnexpectedErrorIsAGeneric500ThatLeaksNothing() throws Exception {
        mvc.perform(get("/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/internal-error"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.detail").value("Ocurrió un error inesperado."))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("abc123"))));
    }

    @Test
    void aDatabaseFailureIsA503ThatLeaksNothing() throws Exception {
        mvc.perform(get("/db"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/service-unavailable"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secreto"))));
    }

    @Test
    void anOversizedUploadIsA413() throws Exception {
        mvc.perform(get("/big"))
            .andExpect(status().isPayloadTooLarge())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/payload-too-large"))
            .andExpect(jsonPath("$.status").value(413));
    }

    @Test
    void anUnknownRouteIsA404ProblemDetails() throws Exception {
        mvc.perform(get("/no/existe"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/not-found"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void everyProblemTypeUriCanBeOpened() throws Exception {
        mvc.perform(get("/problems/ruc-invalid-workbook"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("/problems/ruc-invalid-workbook"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.description").isNotEmpty());
        mvc.perform(get("/problems"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(pe.edu.ta4.shared.error.ProblemType.values().length));
        mvc.perform(get("/problems/inventado"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }
}

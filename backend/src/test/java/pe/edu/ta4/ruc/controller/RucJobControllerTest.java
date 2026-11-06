package pe.edu.ta4.ruc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.ta4.ruc.dto.FileDownload;
import pe.edu.ta4.ruc.dto.RucJobEventResponse;
import pe.edu.ta4.ruc.dto.RucJobItemResponse;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.dto.RucJobResultsResponse;
import pe.edu.ta4.ruc.model.ItemStatus;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.service.RucJobService;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.GlobalExceptionHandler;
import pe.edu.ta4.shared.error.ProblemIssue;

/** Contrato HTTP de la API de usuario: códigos, cabeceras y errores RFC 9457. */
class RucJobControllerTest {
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final UUID ID = UUID.fromString("11111111-2222-4333-8444-555555555555");

    private RucJobService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RucJobService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RucJobController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static RucJobResponse job(JobStatus status) {
        OffsetDateTime at = OffsetDateTime.parse("2026-11-06T10:00:00Z");
        return new RucJobResponse(ID, status, 20, 0, 0, 0, "ruc.xlsx", null, at, at, null, null);
    }

    @Test
    void startAnswers202WithTheJob() throws Exception {
        when(service.requestStart(ID)).thenReturn(job(JobStatus.LISTO_PARA_ROBOT));

        mvc.perform(post("/api/v1/ruc/jobs/{id}/start", ID))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.status").value("LISTO_PARA_ROBOT"));
    }

    @Test
    void startWhileTheRobotIsRunningIs409() throws Exception {
        when(service.requestStart(ID)).thenThrow(ApiException.conflict("El robot ya está en curso para este trabajo (estado EN_CONSULTA)."));

        mvc.perform(post("/api/v1/ruc/jobs/{id}/start", ID))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/conflict"));
    }

    @Test
    void startOfAnUnknownJobIs404() throws Exception {
        when(service.requestStart(ID)).thenThrow(ApiException.notFound("No existe el trabajo " + ID + "."));

        mvc.perform(post("/api/v1/ruc/jobs/{id}/start", ID)).andExpect(status().isNotFound());
    }

    private static MockMultipartFile excel() {
        return new MockMultipartFile("file", "ruc.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1, 2, 3});
    }

    @Test
    void createAnswers201WithLocationAndTheJob() throws Exception {
        when(service.create(eq("ruc.xlsx"), any())).thenReturn(job(JobStatus.LISTO_PARA_ROBOT));

        mvc.perform(multipart("/api/v1/ruc/jobs").file(excel()))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/v1/ruc/jobs/" + ID))
            .andExpect(jsonPath("$.id").value(ID.toString()))
            .andExpect(jsonPath("$.status").value("LISTO_PARA_ROBOT"))
            .andExpect(jsonPath("$.total").value(20))
            .andExpect(jsonPath("$.createdAt").value("2026-11-06T10:00:00Z"));
    }

    @Test
    void invalidWorkbookIsAProblemDetailsWith422AndEveryIssue() throws Exception {
        when(service.create(any(), any())).thenThrow(ApiException.invalidWorkbook("El Excel tiene 2 problemas.", List.of(
            ProblemIssue.of("#/rows/4/ruc", "RUC_INVALID_CHECK_DIGIT", "Fila 4: el dígito verificador no coincide."),
            ProblemIssue.of("#/rows/6/ruc", "RUC_DUPLICATE", "Fila 6: duplicado."))));

        mvc.perform(multipart("/api/v1/ruc/jobs").file(excel()))
            .andExpect(status().isUnprocessableContent())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/ruc-invalid-workbook"))
            .andExpect(jsonPath("$.title").value("Excel de RUC inválido"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.detail").value("El Excel tiene 2 problemas."))
            .andExpect(jsonPath("$.instance").value("/api/v1/ruc/jobs"))
            .andExpect(jsonPath("$.traceId").isNotEmpty())
            .andExpect(jsonPath("$.errors.length()").value(2))
            .andExpect(jsonPath("$.errors[0].pointer").value("#/rows/4/ruc"))
            .andExpect(jsonPath("$.errors[0].code").value("RUC_INVALID_CHECK_DIGIT"))
            .andExpect(jsonPath("$.errors[1].code").value("RUC_DUPLICATE"));
    }

    @Test
    void missingFilePartIsA400ProblemDetails() throws Exception {
        mvc.perform(multipart("/api/v1/ruc/jobs"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void getAnUnknownJobIs404() throws Exception {
        when(service.get(ID)).thenThrow(ApiException.notFound("No existe el trabajo " + ID + "."));

        mvc.perform(get("/api/v1/ruc/jobs/{id}", ID))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/not-found"))
            .andExpect(jsonPath("$.instance").value("/api/v1/ruc/jobs/" + ID));
    }

    @Test
    void aMalformedIdIs400NotA500() throws Exception {
        mvc.perform(get("/api/v1/ruc/jobs/{id}", "no-es-un-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/validation-error"));
    }

    @Test
    void listPassesTheLimit() throws Exception {
        when(service.list(5)).thenReturn(List.of(job(JobStatus.COMPLETADO)));

        mvc.perform(get("/api/v1/ruc/jobs").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("COMPLETADO"));
        verify(service).list(5);
    }

    @Test
    void eventsAndResultsAreReturnedAsJson() throws Exception {
        when(service.events(ID)).thenReturn(List.of(
            new RucJobEventResponse(1, OffsetDateTime.parse("2026-11-06T10:00:00Z"), null, JobStatus.RECIBIDO, "Excel recibido")));
        when(service.results(ID)).thenReturn(new RucJobResultsResponse(ID, JobStatus.COMPLETADO, List.of(
            new RucJobItemResponse(2, "20100047218", ItemStatus.ENCONTRADO, "BANCO", "DIR", "HABIDO", "ACTIVO",
                java.time.LocalDate.of(1889, 4, 9), null))));

        mvc.perform(get("/api/v1/ruc/jobs/{id}/events", ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].to").value("RECIBIDO"));
        mvc.perform(get("/api/v1/ruc/jobs/{id}/results", ID))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.items[0].ruc").value("20100047218"))
            .andExpect(jsonPath("$.items[0].condicionContribuyente").value("HABIDO"))
            .andExpect(jsonPath("$.items[0].fechaInicioActividades").value("1889-04-09"));
    }

    @Test
    void exportIsAnAttachmentThatMustNotBeCached() throws Exception {
        when(service.export(ID)).thenReturn(new FileDownload("ruc_resultado_11111111.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {'P', 'K'}));

        mvc.perform(get("/api/v1/ruc/jobs/{id}/export", ID))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"ruc_resultado_11111111.xlsx\"")))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(content().bytes(new byte[] {'P', 'K'}));
    }

    @Test
    void exportBeforeCompletionIs409() throws Exception {
        when(service.export(ID)).thenThrow(ApiException.conflict("El trabajo aún no tiene resultados para exportar."));

        mvc.perform(get("/api/v1/ruc/jobs/{id}/export", ID))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/conflict"));
    }

    @Test
    void resultFileIsAZipAttachment() throws Exception {
        when(service.resultFile(ID)).thenReturn(new FileDownload("resultado_sunat_11111111.zip", "application/zip", new byte[] {'P', 'K'}));

        mvc.perform(get("/api/v1/ruc/jobs/{id}/result-file", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/zip"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"resultado_sunat_11111111.zip\"")));
    }

    @Test
    void anUnsupportedMethodIs405() throws Exception {
        mvc.perform(post("/api/v1/ruc/jobs/{id}", ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/method-not-allowed"));
    }
}

package pe.edu.ta4.ruc.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.service.RucJobService;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.GlobalExceptionHandler;
import pe.edu.ta4.shared.error.ProblemIssue;

/** Contrato HTTP de lo que usa el robot de UiPath (y la pantalla web como respaldo manual). */
class RucJobDeliveryControllerTest {
    private static final String PROBLEM_JSON = "application/problem+json";
    private static final UUID ID = UUID.fromString("11111111-2222-4333-8444-555555555555");

    private RucJobService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RucJobService.class);
        mvc = MockMvcBuilders.standaloneSetup(new RucJobDeliveryController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static RucJobResponse job(JobStatus status) {
        OffsetDateTime at = OffsetDateTime.parse("2026-11-06T10:00:00Z");
        return new RucJobResponse(ID, status, 20, 20, 0, 0, "ruc.xlsx", "20 de 20 RUC con datos de SUNAT.", at, at, at, null);
    }

    private static MockMultipartFile zip() {
        return new MockMultipartFile("file", "RM1.zip", "application/zip", new byte[] {'P', 'K'});
    }

    @Test
    void inputIsTheTxtForSunatAndIsNotCached() throws Exception {
        when(service.input(ID)).thenReturn(new FileDownload("consulta_ruc_11111111.txt", "text/plain; charset=utf-8",
            "20100047218|\r\n".getBytes()));

        mvc.perform(get("/api/v1/ruc/jobs/{id}/input", ID))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/plain"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment; filename=\"consulta_ruc_11111111.txt\"")))
            .andExpect(content().string("20100047218|\r\n"));
    }

    @Test
    void statusDelegatesToTheService() throws Exception {
        when(service.reportStatus(ID, JobStatus.REQUIERE_INTERVENCION, "Hay una verificación"))
            .thenReturn(job(JobStatus.REQUIERE_INTERVENCION));

        mvc.perform(post("/api/v1/ruc/jobs/{id}/status", ID).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REQUIERE_INTERVENCION\",\"message\":\"Hay una verificación\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REQUIERE_INTERVENCION"));
    }

    @Test
    void statusWithoutAStateIsA400WithAPointerToTheField() throws Exception {
        mvc.perform(post("/api/v1/ruc/jobs/{id}/status", ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/validation-error"))
            .andExpect(jsonPath("$.errors[0].pointer").value("#/status"))
            .andExpect(jsonPath("$.errors[0].code").value("INVALID_FIELD"))
            .andExpect(jsonPath("$.traceId").isNotEmpty());
        verify(service, never()).reportStatus(any(), any(), any());
    }

    @Test
    void statusWithTooLongAMessageIsA400() throws Exception {
        String longMessage = "x".repeat(501);

        mvc.perform(post("/api/v1/ruc/jobs/{id}/status", ID).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EN_CONSULTA\",\"message\":\"" + longMessage + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].pointer").value("#/message"));
    }

    @Test
    void statusWithMalformedJsonIsA400() throws Exception {
        mvc.perform(post("/api/v1/ruc/jobs/{id}/status", ID).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("/problems/validation-error"));
    }

    @Test
    void statusWithAnUnknownStateIsA400() throws Exception {
        mvc.perform(post("/api/v1/ruc/jobs/{id}/status", ID).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"VOLANDO\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void statusWithAnIllegalTransitionIs409() throws Exception {
        when(service.reportStatus(eq(ID), any(), any())).thenThrow(ApiException.conflict("El trabajo está en COMPLETADO."));

        mvc.perform(post("/api/v1/ruc/jobs/{id}/status", ID).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EN_CONSULTA\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type").value("/problems/conflict"));
    }

    @Test
    void resultDeliveryReturnsTheCompletedJob() throws Exception {
        when(service.receiveResult(eq(ID), any())).thenReturn(job(JobStatus.COMPLETADO));

        mvc.perform(multipart("/api/v1/ruc/jobs/{id}/result", ID).file(zip()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETADO"))
            .andExpect(jsonPath("$.foundCount").value(20));
    }

    @Test
    void aSecondDeliveryIs409() throws Exception {
        when(service.receiveResult(eq(ID), any())).thenThrow(ApiException.conflict("El resultado de este trabajo ya fue entregado."));

        mvc.perform(multipart("/api/v1/ruc/jobs/{id}/result", ID).file(zip()))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
            .andExpect(jsonPath("$.detail").value("El resultado de este trabajo ya fue entregado."));
    }

    @Test
    void anInvalidZipIs422WithItsIssues() throws Exception {
        when(service.receiveResult(eq(ID), any())).thenThrow(ApiException.invalidResultFile("El archivo no es un ZIP válido.",
            List.of(ProblemIssue.of("NOT_A_ZIP", "El archivo no es un ZIP válido."))));

        mvc.perform(multipart("/api/v1/ruc/jobs/{id}/result", ID).file(zip()))
            .andExpect(status().isUnprocessableContent())
            .andExpect(jsonPath("$.type").value("/problems/ruc-invalid-result-file"))
            .andExpect(jsonPath("$.errors[0].code").value("NOT_A_ZIP"));
    }

    @Test
    void resultWithoutAFileIsA400() throws Exception {
        mvc.perform(multipart("/api/v1/ruc/jobs/{id}/result", ID))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("/problems/validation-error"));
        verify(service, never()).receiveResult(any(), any());
    }
}

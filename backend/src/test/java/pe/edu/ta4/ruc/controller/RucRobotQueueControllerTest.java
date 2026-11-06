package pe.edu.ta4.ruc.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.service.RucJobService;
import pe.edu.ta4.ruc.service.impl.RobotPresenceServiceImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.GlobalExceptionHandler;

/** La cola que consulta el lanzador local del robot. */
class RucRobotQueueControllerTest {
    private static final UUID ID = UUID.fromString("11111111-2222-4333-8444-555555555555");

    /** Reloj que se puede adelantar a mano. */
    private static final class MutableClock extends java.time.Clock {
        private java.time.Instant now = java.time.Instant.parse("2026-11-06T10:00:00Z");

        void advance(java.time.Duration by) { now = now.plus(by); }

        @Override public java.time.ZoneId getZone() { return java.time.ZoneOffset.UTC; }
        @Override public java.time.Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public java.time.Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();
    private RobotPresenceServiceImpl presence;
    private RucJobService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(RucJobService.class);
        presence = new RobotPresenceServiceImpl(clock);
        mvc = MockMvcBuilders.standaloneSetup(new RucRobotQueueController(service, presence))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void statusIsOfflineUntilTheLauncherAsksForTheQueue() throws Exception {
        mvc.perform(get("/api/v1/ruc/robot/status"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.online").value(false))
            .andExpect(jsonPath("$.lastSeenAt").doesNotExist());
    }

    @Test
    void askingForTheQueueMakesTheRobotOnlineAndSilenceTakesItOffline() throws Exception {
        when(service.pendingStarts()).thenReturn(List.of());

        mvc.perform(get("/api/v1/ruc/robot/pending-start")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/ruc/robot/status"))
            .andExpect(jsonPath("$.online").value(true))
            .andExpect(jsonPath("$.lastSeenAt").exists());

        clock.advance(java.time.Duration.ofSeconds(9));
        mvc.perform(get("/api/v1/ruc/robot/status")).andExpect(jsonPath("$.online").value(false));
    }

    private static RucJobResponse job() {
        OffsetDateTime at = OffsetDateTime.parse("2026-11-06T10:00:00Z");
        return new RucJobResponse(ID, JobStatus.LISTO_PARA_ROBOT, 20, 0, 0, 0, "ruc.xlsx", null, at, at, null, at);
    }

    @Test
    void pendingListsTheQueueAndIsNotCached() throws Exception {
        when(service.pendingStarts()).thenReturn(List.of(job()));

        mvc.perform(get("/api/v1/ruc/robot/pending-start"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(ID.toString()))
            .andExpect(jsonPath("$[0].startRequestedAt").value("2026-11-06T10:00:00Z"));
    }

    @Test
    void pendingIsAnEmptyListWhenNothingWaits() throws Exception {
        when(service.pendingStarts()).thenReturn(List.of());

        mvc.perform(get("/api/v1/ruc/robot/pending-start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void claimReturnsTheJob() throws Exception {
        when(service.claimStart(ID)).thenReturn(job());

        mvc.perform(post("/api/v1/ruc/robot/jobs/{id}/claim", ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    void claimTwiceIs409() throws Exception {
        when(service.claimStart(ID)).thenThrow(ApiException.conflict("No hay un arranque pendiente para este trabajo."));

        mvc.perform(post("/api/v1/ruc/robot/jobs/{id}/claim", ID))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.type").value("/problems/conflict"));
    }
}

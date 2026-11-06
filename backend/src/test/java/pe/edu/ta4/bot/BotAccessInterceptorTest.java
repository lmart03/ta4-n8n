package pe.edu.ta4.bot;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.ta4.bot.config.BotAccessInterceptor;
import pe.edu.ta4.bot.controller.BotPanelController;
import pe.edu.ta4.bot.service.BotChatService;
import pe.edu.ta4.bot.service.BotMetricsService;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.bot.service.BotStatusService;
import pe.edu.ta4.bot.service.BotStreamService;
import pe.edu.ta4.bot.service.BotTestService;
import pe.edu.ta4.bot.service.BotWebhookService;
import pe.edu.ta4.shared.error.GlobalExceptionHandler;

/** El panel falla cerrado: sin clave configurada o con clave incorrecta no se entra. */
class BotAccessInterceptorTest {
    private static BotProperties properties(String admin) {
        return new BotProperties(admin, "ingest-secret", "", "", "", "gemini", "http://n8n", "http://t", "/webhook/x");
    }

    private MockMvc mvc(String adminKey) {
        BotChatService chats = mock(BotChatService.class);
        BotPanelController controller = new BotPanelController(mock(BotStatusService.class), mock(BotWebhookService.class),
            chats, mock(BotStreamService.class), mock(BotSettingsService.class),
            mock(BotTestService.class), mock(BotMetricsService.class));
        return MockMvcBuilders.standaloneSetup(controller, new pe.edu.ta4.bot.controller.BotIngestController(chats)).setControllerAdvice(new GlobalExceptionHandler())
            .addInterceptors(new BotAccessInterceptor(properties(adminKey), Clock.systemUTC())).build();
    }

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = mvc("clave-de-prueba-larga");
    }

    @Test
    void sinClaveResponde401ProblemJson() throws Exception {
        mvc.perform(post("/api/v1/bot/session")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.type").value("/problems/unauthorized"));
    }

    @Test
    void conClaveIncorrectaResponde401() throws Exception {
        mvc.perform(post("/api/v1/bot/session").header("X-Admin-Key", "otra")).andExpect(status().isUnauthorized());
    }

    @Test
    void conClaveCorrectaEntra() throws Exception {
        mvc.perform(post("/api/v1/bot/session").header("X-Admin-Key", "clave-de-prueba-larga"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.ok").value(true));
    }

    @Test
    void sinClaveConfiguradaElPanelQuedaCerrado() throws Exception {
        mvc("").perform(post("/api/v1/bot/session").header("X-Admin-Key", "")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("deshabilitado")));
    }

    @Test
    void laClaveDeAdminNoSirveParaLasRutasDeIngesta() throws Exception {
        mvc.perform(post("/api/v1/bot/ingest/incoming").header("X-Admin-Key", "clave-de-prueba-larga"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void tras10IntentosFallidosBloquea429() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(get("/api/v1/bot/status").header("X-Admin-Key", "mala")).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/bot/status").header("X-Admin-Key", "mala")).andExpect(status().isTooManyRequests());
    }
}

package pe.edu.ta4.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.ta4.bot.dto.BotStatusResponse;
import pe.edu.ta4.bot.dto.ChatSummary;
import pe.edu.ta4.bot.dto.GeminiPingResponse;
import pe.edu.ta4.bot.dto.HumanMessageRequest;
import pe.edu.ta4.bot.dto.MessageView;
import pe.edu.ta4.bot.dto.MetricsResponse;
import pe.edu.ta4.bot.dto.ModeRequest;
import pe.edu.ta4.bot.dto.SessionResponse;
import pe.edu.ta4.bot.dto.SettingsDto;
import pe.edu.ta4.bot.dto.StreamTicketResponse;
import pe.edu.ta4.bot.dto.TestCaseResult;
import pe.edu.ta4.bot.dto.TestRequest;
import pe.edu.ta4.bot.dto.TestRunResponse;
import pe.edu.ta4.bot.dto.WebhookResult;
import pe.edu.ta4.bot.service.BotChatService;
import pe.edu.ta4.bot.service.BotMetricsService;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.bot.service.BotStatusService;
import pe.edu.ta4.bot.service.BotStreamService;
import pe.edu.ta4.bot.service.BotTestService;
import pe.edu.ta4.bot.service.BotWebhookService;

/** API del panel del bot. Todas las rutas (salvo el flujo en vivo) exigen el header {@code X-Admin-Key}. */
@RestController
@RequestMapping("/api/v1/bot")
@Tag(name = "Panel del bot")
public class BotPanelController {
    private final BotStatusService status;
    private final BotWebhookService webhook;
    private final BotChatService chats;
    private final BotStreamService stream;
    private final BotSettingsService settings;
    private final BotTestService tests;
    private final BotMetricsService metrics;

    public BotPanelController(BotStatusService status, BotWebhookService webhook, BotChatService chats, BotStreamService stream,
                              BotSettingsService settings, BotTestService tests, BotMetricsService metrics) {
        this.status = status;
        this.webhook = webhook;
        this.chats = chats;
        this.stream = stream;
        this.settings = settings;
        this.tests = tests;
        this.metrics = metrics;
    }

    @PostMapping("/session")
    @Operation(summary = "Validar la clave de administrador (la valida el interceptor; aquí solo se confirma)")
    public SessionResponse session() {
        return new SessionResponse(true);
    }

    @PostMapping("/stream-ticket")
    @Operation(summary = "Ticket de un solo uso para abrir el flujo en vivo")
    public StreamTicketResponse streamTicket() {
        return stream.issueTicket();
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    @Operation(summary = "Flujo en vivo (SSE): eventos message, chat, chat-deleted y test-progress")
    public SseEmitter stream(@RequestParam String ticket) {
        return stream.subscribe(ticket);
    }

    @GetMapping("/status")
    @Operation(summary = "Estado del túnel, webhook, n8n, Gemini y datos")
    public BotStatusResponse status() {
        return status.status();
    }

    @PostMapping("/webhook/register")
    @Operation(summary = "Registrar el webhook de Telegram con la URL actual del túnel")
    public WebhookResult registerWebhook() {
        return webhook.register();
    }

    @PostMapping("/gemini/ping")
    @Operation(summary = "Probar Gemini con una consulta mínima")
    public GeminiPingResponse pingGemini() {
        return status.pingGemini();
    }

    @GetMapping("/chats")
    @Operation(summary = "Chats recientes")
    public List<ChatSummary> chats() {
        return chats.chats();
    }

    @GetMapping("/chats/{id}/messages")
    @Operation(summary = "Mensajes de un chat, del más nuevo al más viejo")
    public List<MessageView> messages(@PathVariable UUID id,
                                      @RequestParam(required = false) OffsetDateTime before,
                                      @RequestParam(defaultValue = "50") int limit) {
        return chats.messages(id, before, limit);
    }

    @PostMapping("/chats/{id}/messages")
    @Operation(summary = "Responder como persona: se envía por Telegram y el chat pasa a modo humano")
    public MessageView reply(@PathVariable UUID id, @Valid @RequestBody HumanMessageRequest request) {
        return chats.sendHuman(id, request.text());
    }

    @PatchMapping("/chats/{id}/mode")
    @Operation(summary = "Cambiar quién responde en el chat: BOT o HUMANO")
    public ChatSummary mode(@PathVariable UUID id, @Valid @RequestBody ModeRequest request) {
        return chats.setMode(id, request.mode());
    }

    @DeleteMapping("/chats/{id}")
    @Operation(summary = "Borrar un chat y sus mensajes")
    public void delete(@PathVariable UUID id) {
        chats.delete(id);
    }

    @GetMapping("/settings")
    @Operation(summary = "Ajustes: guardar conversaciones, retención y registro automático")
    public SettingsDto settings() {
        return settings.get();
    }

    @PutMapping("/settings")
    public SettingsDto updateSettings(@Valid @RequestBody SettingsDto request) {
        return settings.update(request);
    }

    @PostMapping("/test")
    @Operation(summary = "Enviar una pregunta de prueba por el webhook real de n8n y esperar la respuesta")
    public TestCaseResult test(@Valid @RequestBody TestRequest request) {
        return tests.ask(request.question());
    }

    @PostMapping("/test-suite")
    @Operation(summary = "Ejecutar la matriz de pruebas PR-BOT en segundo plano")
    public TestRunResponse startSuite() {
        return tests.startSuite();
    }

    @GetMapping("/test-suite")
    @Operation(summary = "Última ejecución de la matriz de pruebas")
    public TestRunResponse lastSuite() {
        return tests.lastRun();
    }

    @GetMapping("/metrics")
    @Operation(summary = "Métricas de respuestas del bot (sin contenido), sin contar las pruebas")
    public MetricsResponse metrics(@RequestParam(defaultValue = "24") int hours) {
        return metrics.metrics(hours);
    }
}

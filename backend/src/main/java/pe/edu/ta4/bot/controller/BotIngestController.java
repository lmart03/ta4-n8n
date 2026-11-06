package pe.edu.ta4.bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.ta4.bot.dto.ErrorReport;
import pe.edu.ta4.bot.dto.IncomingRequest;
import pe.edu.ta4.bot.dto.IncomingResponse;
import pe.edu.ta4.bot.dto.OutgoingRequest;
import pe.edu.ta4.bot.service.BotChatService;

/** Rutas que usa n8n para avisar al backend. Exigen {@code X-Ingest-Key}. */
@RestController
@RequestMapping("/api/v1/bot/ingest")
@Tag(name = "Panel del bot")
public class BotIngestController {
    private static final Logger log = LoggerFactory.getLogger(BotIngestController.class);

    private final BotChatService chats;

    public BotIngestController(BotChatService chats) {
        this.chats = chats;
    }

    @PostMapping("/incoming")
    @Operation(summary = "n8n recibió un mensaje; responde el modo del chat (BOT o HUMANO)")
    public IncomingResponse incoming(@Valid @RequestBody IncomingRequest request) {
        return chats.incoming(request);
    }

    @PostMapping("/outgoing")
    @Operation(summary = "n8n respondió (o intentó responder) a Telegram")
    public void outgoing(@Valid @RequestBody OutgoingRequest request) {
        chats.outgoing(request);
    }

    @PostMapping("/error")
    @Operation(summary = "Error del flujo de n8n; solo queda en el log del backend, sin datos del usuario")
    public void error(@Valid @RequestBody ErrorReport report) {
        log.warn("Error en el flujo de n8n (nodo {}): {}", report.node(), report.message());
    }
}

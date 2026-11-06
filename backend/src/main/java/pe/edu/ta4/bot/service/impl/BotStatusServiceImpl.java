package pe.edu.ta4.bot.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.bot.client.GeminiClient;
import pe.edu.ta4.bot.client.N8nClient;
import pe.edu.ta4.bot.client.TelegramClient;
import pe.edu.ta4.bot.client.TunnelClient;
import pe.edu.ta4.bot.dto.BotStatusResponse;
import pe.edu.ta4.bot.dto.ComponentStatus;
import pe.edu.ta4.bot.dto.GeminiPingResponse;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.bot.service.BotStatusService;
import pe.edu.ta4.business.service.BusinessService;
import pe.edu.ta4.faq.service.FaqService;
import pe.edu.ta4.shared.error.ApiException;

@Service
public class BotStatusServiceImpl implements BotStatusService {
    private static final Duration STATUS_TTL = Duration.ofSeconds(4);
    private static final Duration IDENTITY_TTL = Duration.ofMinutes(10);

    private final BotProperties properties;
    private final TunnelClient tunnel;
    private final TelegramClient telegram;
    private final N8nClient n8n;
    private final GeminiClient gemini;
    private final BotSettingsService settings;
    private final FaqService faqs;
    private final BusinessService business;
    private final Clock clock;

    private BotStatusResponse cached;
    private Instant cachedAt = Instant.EPOCH;
    private Optional<TelegramClient.BotIdentity> identity = Optional.empty();
    private Instant identityAt = Instant.EPOCH;
    private ComponentStatus lastGemini;

    public BotStatusServiceImpl(BotProperties properties, TunnelClient tunnel, TelegramClient telegram, N8nClient n8n,
                                GeminiClient gemini, BotSettingsService settings, FaqService faqs, BusinessService business,
                                Clock clock) {
        this.properties = properties;
        this.tunnel = tunnel;
        this.telegram = telegram;
        this.n8n = n8n;
        this.gemini = gemini;
        this.settings = settings;
        this.faqs = faqs;
        this.business = business;
        this.clock = clock;
    }

    @Override
    public synchronized BotStatusResponse status() {
        Instant now = clock.instant();
        if (cached != null && Duration.between(cachedAt, now).compareTo(STATUS_TTL) < 0) return cached;
        cached = compute(now);
        cachedAt = now;
        return cached;
    }

    @Override
    public synchronized GeminiPingResponse pingGemini() {
        long started = System.nanoTime();
        try {
            String text = gemini.ping();
            long ms = (System.nanoTime() - started) / 1_000_000;
            lastGemini = new ComponentStatus("gemini", "Gemini", "OK",
                "Modelo " + properties.geminiModel() + " respondió en " + ms + " ms.");
            cachedAt = Instant.EPOCH;
            return new GeminiPingResponse(true, text.isBlank() ? "Respondió sin texto." : "Respondió: " + text, ms);
        } catch (ApiException e) {
            long ms = (System.nanoTime() - started) / 1_000_000;
            lastGemini = new ComponentStatus("gemini", "Gemini", "ERROR", e.getMessage());
            cachedAt = Instant.EPOCH;
            return new GeminiPingResponse(false, e.getMessage(), ms);
        }
    }

    private BotStatusResponse compute(Instant now) {
        List<ComponentStatus> components = new ArrayList<>();
        Optional<String> tunnelUrl = tunnel.publicUrl();
        components.add(tunnelUrl
            .map(url -> new ComponentStatus("tunnel", "Túnel público", "OK", url))
            .orElseGet(() -> new ComponentStatus("tunnel", "Túnel público", "ERROR",
                "El contenedor cloudflared no responde. Levántalo con: docker compose -f infra/compose.yaml up -d cloudflared")));

        components.add(webhookComponent(tunnelUrl));
        components.add(n8nComponent());
        components.add(geminiComponent());
        components.add(dataComponent());

        if (now.isAfter(identityAt.plus(IDENTITY_TTL)) || identity.isEmpty() && now.isAfter(identityAt.plusSeconds(20))) {
            identity = telegram.me();
            identityAt = now;
        }
        String username = identity.map(TelegramClient.BotIdentity::username).orElse(null);
        String link = username == null || username.isBlank() ? null : "https://t.me/" + username;
        return new BotStatusResponse(components, tunnelUrl.orElse(null), username, link,
            settings.get().autoRegisterWebhook(), OffsetDateTime.now(clock));
    }

    private ComponentStatus webhookComponent(Optional<String> tunnelUrl) {
        if (!properties.telegramConfigured()) {
            return new ComponentStatus("webhook", "Webhook de Telegram", "ERROR", "Falta TELEGRAM_BOT_TOKEN en infra/.env.");
        }
        try {
            TelegramClient.WebhookInfo info = telegram.webhookInfo();
            if (info.url().isBlank()) {
                return new ComponentStatus("webhook", "Webhook de Telegram", "ERROR",
                    "No hay webhook registrado. Pulsa «Registrar webhook».");
            }
            String expected = tunnelUrl.map(url -> url + properties.webhookPath()).orElse(null);
            if (expected != null && !expected.equals(info.url())) {
                return new ComponentStatus("webhook", "Webhook de Telegram", "WARN",
                    "Apunta a otra dirección (túnel anterior). Pulsa «Registrar webhook».");
            }
            if (!info.lastErrorMessage().isBlank()) {
                return new ComponentStatus("webhook", "Webhook de Telegram", "WARN",
                    "Registrado, pero Telegram reporta: " + info.lastErrorMessage());
            }
            return new ComponentStatus("webhook", "Webhook de Telegram", "OK",
                "Registrado" + (info.pendingUpdateCount() > 0 ? " · " + info.pendingUpdateCount() + " pendientes" : "") + ".");
        } catch (ApiException e) {
            return new ComponentStatus("webhook", "Webhook de Telegram", "ERROR", e.getMessage());
        }
    }

    private ComponentStatus n8nComponent() {
        N8nClient.Health health = n8n.health();
        if (!health.reachable()) {
            return new ComponentStatus("n8n", "n8n", "ERROR", "n8n no responde en la red de Compose.");
        }
        if (!health.workflowActive()) {
            return new ComponentStatus("n8n", "n8n", "ERROR", "El workflow del bot está inactivo. Publícalo en n8n.");
        }
        return new ComponentStatus("n8n", "n8n", "OK", "Workflow del bot activo.");
    }

    private ComponentStatus geminiComponent() {
        if (!properties.geminiConfigured()) {
            return new ComponentStatus("gemini", "Gemini", "WARN",
                "Sin GEMINI_API_KEY: el bot responderá solo con las FAQ.");
        }
        if (lastGemini != null) return lastGemini;
        return new ComponentStatus("gemini", "Gemini", "OK",
            "Clave configurada · modelo " + properties.geminiModel() + ". Usa «Probar Gemini» para comprobarla.");
    }

    private ComponentStatus dataComponent() {
        try {
            int count = faqs.list(true).size();
            business.getBusiness();
            return new ComponentStatus("data", "Datos de la clínica", count > 0 ? "OK" : "WARN",
                count + " preguntas frecuentes activas.");
        } catch (ApiException e) {
            return new ComponentStatus("data", "Datos de la clínica", "ERROR", e.getMessage());
        }
    }
}

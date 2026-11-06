package pe.edu.ta4.bot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.bot.client.TelegramClient;
import pe.edu.ta4.bot.client.TunnelClient;
import pe.edu.ta4.bot.dto.WebhookResult;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.bot.service.BotWebhookService;
import pe.edu.ta4.shared.error.ApiException;

@Service
public class BotWebhookServiceImpl implements BotWebhookService {
    private static final Logger log = LoggerFactory.getLogger(BotWebhookServiceImpl.class);

    private final BotProperties properties;
    private final TunnelClient tunnel;
    private final TelegramClient telegram;
    private final BotSettingsService settings;

    public BotWebhookServiceImpl(BotProperties properties, TunnelClient tunnel, TelegramClient telegram,
                                 BotSettingsService settings) {
        this.properties = properties;
        this.tunnel = tunnel;
        this.telegram = telegram;
        this.settings = settings;
    }

    @Override
    public WebhookResult register() {
        if (!properties.telegramConfigured()) {
            throw ApiException.unavailable("Falta TELEGRAM_BOT_TOKEN en infra/.env.");
        }
        if (properties.webhookSecret().isBlank() || "pendiente".equalsIgnoreCase(properties.webhookSecret())) {
            throw ApiException.unavailable("Falta TELEGRAM_WEBHOOK_SECRET en infra/.env.");
        }
        String base = tunnel.publicUrl().orElseThrow(() ->
            ApiException.unavailable("El túnel público no está activo: levanta el contenedor cloudflared."));
        String url = base + properties.webhookPath();
        telegram.setWebhook(url, properties.webhookSecret());
        log.info("Webhook de Telegram registrado en {}", url);
        return new WebhookResult(url, true, "Webhook registrado. Telegram enviará los mensajes a n8n por el túnel.");
    }

    @Override
    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void autoRegister() {
        try {
            if (!settings.get().autoRegisterWebhook() || !properties.telegramConfigured()) return;
            String base = tunnel.publicUrl().orElse(null);
            if (base == null) return;
            if ((base + properties.webhookPath()).equals(telegram.webhookInfo().url())) return;
            register();
        } catch (ApiException e) {
            log.warn("Registro automático del webhook omitido: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Registro automático del webhook falló: {}", e.getClass().getSimpleName());
        }
    }
}

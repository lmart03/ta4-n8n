package pe.edu.ta4.bot.client.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.bot.client.TelegramClient;
import pe.edu.ta4.shared.error.ApiException;

@Component
public class TelegramHttpClient implements TelegramClient {
    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() { };

    private final BotProperties properties;
    private final RestClient http = HttpSupport.client("https://api.telegram.org", Duration.ofSeconds(8));

    public TelegramHttpClient(BotProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<BotIdentity> me() {
        if (!properties.telegramConfigured()) return Optional.empty();
        try {
            Map<String, Object> result = result(call("getMe", null));
            return Optional.of(new BotIdentity(text(result.get("username")), text(result.get("first_name"))));
        } catch (ApiException e) {
            return Optional.empty();
        }
    }

    @Override
    public WebhookInfo webhookInfo() {
        Map<String, Object> result = result(call("getWebhookInfo", null));
        Object pending = result.get("pending_update_count");
        return new WebhookInfo(text(result.get("url")), pending instanceof Number n ? n.intValue() : 0,
            text(result.get("last_error_message")));
    }

    @Override
    public void setWebhook(String url, String secret) {
        call("setWebhook", Map.of("url", url, "secret_token", secret, "allowed_updates", List.of("message"),
            "drop_pending_updates", true));
    }

    @Override
    public void sendMessage(long chatId, String text) {
        call("sendMessage", Map.of("chat_id", chatId, "text", text));
    }

    private Map<String, Object> call(String method, Map<String, Object> body) {
        if (!properties.telegramConfigured()) {
            throw ApiException.unavailable("Falta TELEGRAM_BOT_TOKEN en la configuración del backend.");
        }
        try {
            RestClient.RequestBodySpec spec = http.post().uri("/bot" + properties.telegramToken() + "/" + method)
                .contentType(MediaType.APPLICATION_JSON);
            Map<String, Object> response = (body == null ? spec : spec.body(body)).retrieve().body(JSON);
            if (response == null || !Boolean.TRUE.equals(response.get("ok"))) {
                throw ApiException.badGateway("Telegram rechazó la operación.");
            }
            return response;
        } catch (RestClientResponseException e) {
            // Nunca se incluye la URL (lleva el token) ni el cuerpo completo: solo la descripción de Telegram.
            throw ApiException.badGateway("Telegram respondió " + e.getStatusCode().value() + ": " + description(e));
        } catch (ResourceAccessException e) {
            throw ApiException.badGateway("No se pudo contactar con Telegram. Revisa la conexión a internet.");
        }
    }

    private static String description(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        int at = body.indexOf("\"description\":\"");
        if (at < 0) return "sin detalle";
        int start = at + 15;
        int end = body.indexOf('"', start);
        return end > start ? body.substring(start, end) : "sin detalle";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> result(Map<String, Object> response) {
        Object result = response.get("result");
        return result instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}

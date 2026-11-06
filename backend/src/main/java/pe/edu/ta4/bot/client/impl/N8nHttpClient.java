package pe.edu.ta4.bot.client.impl;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.bot.client.N8nClient;
import pe.edu.ta4.shared.error.ApiException;

@Component
public class N8nHttpClient implements N8nClient {
    private final BotProperties properties;
    private final RestClient http;

    public N8nHttpClient(BotProperties properties) {
        this.properties = properties;
        this.http = HttpSupport.client(properties.n8nBaseUrl(), Duration.ofSeconds(4));
    }

    @Override
    public Health health() {
        try {
            http.get().uri("/healthz").retrieve().toBodilessEntity();
        } catch (RuntimeException e) {
            return new Health(false, false);
        }
        try {
            // Un POST vacío: con el workflow activo n8n responde 200 (descarta por secreto); inactivo, 404.
            http.post().uri(properties.webhookPath()).contentType(MediaType.APPLICATION_JSON).body(Map.of())
                .retrieve().toBodilessEntity();
            return new Health(true, true);
        } catch (RestClientResponseException e) {
            return new Health(true, e.getStatusCode().value() != 404);
        } catch (RuntimeException e) {
            return new Health(false, false);
        }
    }

    @Override
    public void sendTestUpdate(long chatId, String name, String text) {
        Map<String, Object> update = Map.of("update_id", System.currentTimeMillis(), "message", Map.of(
            "message_id", 1,
            "chat", Map.of("id", chatId, "type", "private"),
            "from", Map.of("id", chatId, "first_name", name),
            "text", text));
        try {
            http.post().uri(properties.webhookPath()).contentType(MediaType.APPLICATION_JSON)
                .header("X-Telegram-Bot-Api-Secret-Token", properties.webhookSecret())
                .body(update).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw ApiException.unavailable("n8n respondió " + e.getStatusCode().value()
                + ": revisa que el workflow del bot esté activo.");
        } catch (RuntimeException e) {
            throw ApiException.unavailable("No se pudo contactar con n8n.");
        }
    }
}

package pe.edu.ta4.bot.client.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.bot.client.GeminiClient;
import pe.edu.ta4.shared.error.ApiException;

@Component
public class GeminiHttpClient implements GeminiClient {
    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() { };

    private final BotProperties properties;
    private final RestClient http = HttpSupport.client("https://generativelanguage.googleapis.com", Duration.ofSeconds(20));

    public GeminiHttpClient(BotProperties properties) {
        this.properties = properties;
    }

    @Override
    public String ping() {
        if (!properties.geminiConfigured()) throw ApiException.unavailable("Falta GEMINI_API_KEY en la configuración del backend.");
        Map<String, Object> body = Map.of(
            "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", "Responde solo: ok")))),
            "generationConfig", Map.of("maxOutputTokens", 16));
        try {
            Map<String, Object> response = http.post()
                .uri("/v1beta/models/{model}:generateContent", properties.geminiModel())
                .header("x-goog-api-key", properties.geminiKey())
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JSON);
            return firstText(response);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String hint = switch (status) {
                case 400, 403 -> "la clave no es válida o no tiene acceso al modelo";
                case 404 -> "el modelo '" + properties.geminiModel() + "' no existe";
                case 429 -> "se agotó la cuota del nivel gratuito";
                default -> "error del servicio";
            };
            throw ApiException.badGateway("Gemini respondió " + status + ": " + hint + ".");
        } catch (ResourceAccessException e) {
            throw ApiException.badGateway("No se pudo contactar con Gemini. Revisa la conexión a internet.");
        }
    }

    @SuppressWarnings("unchecked")
    private static String firstText(Map<String, Object> response) {
        if (response == null || !(response.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()) return "";
        Object content = ((Map<String, Object>) candidates.getFirst()).get("content");
        if (!(content instanceof Map<?, ?> map) || !(map.get("parts") instanceof List<?> parts) || parts.isEmpty()) return "";
        Object text = ((Map<String, Object>) parts.getFirst()).get("text");
        return text == null ? "" : text.toString().trim();
    }
}

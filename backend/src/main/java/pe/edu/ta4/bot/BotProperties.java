package pe.edu.ta4.bot;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del panel del bot ({@code ta4.bot.*}). Los secretos llegan por variables de entorno y nunca se
 * devuelven por la API: solo se informa si están o no configurados.
 *
 * @param adminKey         clave del panel web (header {@code X-Admin-Key}); vacía = panel deshabilitado
 * @param ingestKey        clave que usa n8n para avisar al backend (header {@code X-Ingest-Key})
 * @param telegramToken    token de BotFather
 * @param webhookSecret    secreto del webhook de Telegram (el mismo que valida n8n)
 * @param geminiKey        clave de la API de Gemini
 * @param geminiModel      modelo de Gemini
 * @param n8nBaseUrl       dirección interna de n8n en la red de Compose
 * @param tunnelMetricsUrl dirección interna de cloudflared (endpoint de métricas con la URL pública)
 * @param webhookPath      ruta del webhook en n8n
 */
@ConfigurationProperties(prefix = "ta4.bot")
public record BotProperties(
    @DefaultValue("") String adminKey,
    @DefaultValue("") String ingestKey,
    @DefaultValue("") String telegramToken,
    @DefaultValue("") String webhookSecret,
    @DefaultValue("") String geminiKey,
    @DefaultValue("gemini-3.5-flash-lite") String geminiModel,
    @DefaultValue("http://n8n:5678") String n8nBaseUrl,
    @DefaultValue("http://cloudflared:2000") String tunnelMetricsUrl,
    @DefaultValue("/webhook/telegram-ta4") String webhookPath) {

    public boolean telegramConfigured() { return real(telegramToken); }

    public boolean geminiConfigured() { return real(geminiKey); }

    public boolean adminKeyConfigured() { return real(adminKey); }

    public boolean ingestKeyConfigured() { return real(ingestKey); }

    private static boolean real(String value) {
        return value != null && !value.isBlank() && !"pendiente".equalsIgnoreCase(value.trim());
    }
}

package pe.edu.ta4.bot.client;

import java.util.Optional;

/** API de bots de Telegram. Los errores se traducen a {@code ApiException} (502/503), nunca con el token. */
public interface TelegramClient {
    record BotIdentity(String username, String name) {
    }

    record WebhookInfo(String url, int pendingUpdateCount, String lastErrorMessage) {
    }

    /** Identidad del bot (getMe); vacío si no hay token o Telegram no responde. */
    Optional<BotIdentity> me();

    /** Webhook registrado actualmente (getWebhookInfo). */
    WebhookInfo webhookInfo();

    /** Registra el webhook con el secreto de cabecera y solo los mensajes como tipo de actualización. */
    void setWebhook(String url, String secret);

    /** Envía un mensaje de texto al chat. */
    void sendMessage(long chatId, String text);
}

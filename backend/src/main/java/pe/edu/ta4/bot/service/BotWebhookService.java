package pe.edu.ta4.bot.service;

import pe.edu.ta4.bot.dto.WebhookResult;

/** Registro del webhook de Telegram con la URL pública del túnel. */
public interface BotWebhookService {
    /** Registra el webhook ahora. Falla con 503 si no hay túnel, token o secreto configurados. */
    WebhookResult register();

    /** Revisa cada 30 s si la URL del túnel cambió y, si el ajuste lo permite, vuelve a registrar el webhook. */
    void autoRegister();
}

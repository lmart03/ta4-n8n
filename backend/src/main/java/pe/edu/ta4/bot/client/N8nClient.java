package pe.edu.ta4.bot.client;

/** n8n dentro de la red de Compose. */
public interface N8nClient {
    /**
     * @param reachable      n8n responde en su puerto
     * @param workflowActive el webhook del bot está registrado (workflow activo)
     */
    record Health(boolean reachable, boolean workflowActive) {
    }

    Health health();

    /** Entrega a n8n una actualización de Telegram simulada, con el secreto de cabecera, como si viniera de Telegram. */
    void sendTestUpdate(long chatId, String name, String text);
}

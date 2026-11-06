package pe.edu.ta4.bot;

/** Evento interno: n8n informó una respuesta del bot. Lo escucha la suite de pruebas para esperar su resultado. */
public record BotReplyEvent(long chatId, String text, BotSource source, Integer latencyMs) {
}

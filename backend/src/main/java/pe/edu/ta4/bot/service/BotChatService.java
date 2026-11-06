package pe.edu.ta4.bot.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import pe.edu.ta4.bot.ChatMode;
import pe.edu.ta4.bot.dto.ChatSummary;
import pe.edu.ta4.bot.dto.IncomingRequest;
import pe.edu.ta4.bot.dto.IncomingResponse;
import pe.edu.ta4.bot.dto.MessageView;
import pe.edu.ta4.bot.dto.OutgoingRequest;

/** Conversaciones del bot: lo que n8n informa y lo que el panel consulta o responde. */
public interface BotChatService {
    /** n8n avisa que llegó un mensaje. Devuelve el modo del chat para que n8n sepa si debe responder. */
    IncomingResponse incoming(IncomingRequest request);

    /** n8n avisa que respondió (o intentó responder). Registra el mensaje y la métrica sin contenido. */
    void outgoing(OutgoingRequest request);

    /** Hasta 100 chats, el más reciente primero. */
    List<ChatSummary> chats();

    /** Mensajes de un chat, del más nuevo al más viejo, anteriores a {@code before} si se indica. */
    List<MessageView> messages(UUID chatId, OffsetDateTime before, int limit);

    /** Una persona responde desde el panel: se envía por Telegram y el chat pasa a modo humano. */
    MessageView sendHuman(UUID chatId, String text);

    ChatSummary setMode(UUID chatId, ChatMode mode);

    void delete(UUID chatId);

    /** Borra mensajes y chats vencidos según la retención. Devuelve cuántos mensajes borró. */
    int purgeExpired();
}

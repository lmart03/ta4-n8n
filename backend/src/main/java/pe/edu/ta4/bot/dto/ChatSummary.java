package pe.edu.ta4.bot.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import pe.edu.ta4.bot.BotAuthor;
import pe.edu.ta4.bot.ChatMode;

public record ChatSummary(UUID id, long telegramChatId, String name, String username, ChatMode mode, boolean test,
                         String lastText, BotAuthor lastAuthor, OffsetDateTime lastMessageAt) {
}

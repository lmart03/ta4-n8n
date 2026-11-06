package pe.edu.ta4.bot.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import pe.edu.ta4.bot.BotAuthor;
import pe.edu.ta4.bot.BotSource;

public record MessageView(UUID id, UUID chatId, BotAuthor author, String text, BotSource source, Integer latencyMs,
                         OffsetDateTime createdAt) {
}

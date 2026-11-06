package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.ta4.bot.BotSource;

/** Respuesta que n8n envió (o intentó enviar) a Telegram. */
public record OutgoingRequest(@NotNull Long chatId, @NotBlank @Size(max = 4100) String text, @NotNull BotSource source,
                              @Min(0) Integer latencyMs) {
}

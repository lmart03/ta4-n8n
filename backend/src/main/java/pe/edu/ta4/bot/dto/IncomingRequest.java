package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mensaje recién llegado a n8n desde Telegram. */
public record IncomingRequest(@NotNull Long chatId, @Size(max = 200) String name, @Size(max = 100) String username,
                              @NotBlank @Size(max = 4000) String text) {
}

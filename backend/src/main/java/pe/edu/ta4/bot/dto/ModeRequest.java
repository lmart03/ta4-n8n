package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.NotNull;
import pe.edu.ta4.bot.ChatMode;

public record ModeRequest(@NotNull ChatMode mode) {
}

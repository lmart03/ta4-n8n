package pe.edu.ta4.bot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SettingsDto(boolean storeConversations, @Min(1) @Max(720) int retentionHours,
                         boolean autoRegisterWebhook) {
}

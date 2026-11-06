package pe.edu.ta4.bot.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record BotStatusResponse(List<ComponentStatus> components, String tunnelUrl, String botUsername,
                                String botLink, boolean autoRegister, OffsetDateTime checkedAt) {
}

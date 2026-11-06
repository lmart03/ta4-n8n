package pe.edu.ta4.bot.dto;

public record GeminiPingResponse(boolean ok, String detail, long latencyMs) {
}

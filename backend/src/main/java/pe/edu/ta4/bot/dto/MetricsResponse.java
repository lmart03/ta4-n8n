package pe.edu.ta4.bot.dto;

import java.util.Map;

public record MetricsResponse(int hours, long total, Double avgLatencyMs, Integer p95LatencyMs, Map<String, Long> bySource,
                             double derivedPercent) {
}

package pe.edu.ta4.bot.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.bot.BotSource;
import pe.edu.ta4.bot.dto.MetricsResponse;
import pe.edu.ta4.bot.model.BotMetricEvent;
import pe.edu.ta4.bot.repository.BotMetricEventRepository;
import pe.edu.ta4.bot.service.BotMetricsService;

@Service
@Transactional(readOnly = true)
public class BotMetricsServiceImpl implements BotMetricsService {
    private final BotMetricEventRepository repository;
    private final Clock clock;

    public BotMetricsServiceImpl(BotMetricEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public MetricsResponse metrics(int hours) {
        int range = Math.max(1, Math.min(hours, 720));
        List<BotMetricEvent> events = repository.findByAtAfterAndTestFalse(OffsetDateTime.now(clock).minusHours(range));
        Map<BotSource, Long> counts = new EnumMap<>(BotSource.class);
        events.forEach(event -> counts.merge(event.getSource(), 1L, Long::sum));
        Map<String, Long> bySource = new LinkedHashMap<>();
        for (BotSource source : BotSource.values()) bySource.put(source.name(), counts.getOrDefault(source, 0L));

        List<Integer> latencies = events.stream().map(BotMetricEvent::getLatencyMs).filter(v -> v != null).sorted().toList();
        Double average = latencies.isEmpty() ? null
            : latencies.stream().mapToInt(Integer::intValue).average().orElse(0);
        Integer p95 = latencies.isEmpty() ? null
            : latencies.get(Math.min(latencies.size() - 1, (int) Math.ceil(latencies.size() * 0.95) - 1));
        long total = events.size();
        long derived = counts.getOrDefault(BotSource.DERIVADA, 0L) + counts.getOrDefault(BotSource.RECHAZO, 0L);
        double derivedPercent = total == 0 ? 0 : Math.round(derived * 1000.0 / total) / 10.0;
        return new MetricsResponse(range, total, average == null ? null : Math.round(average * 10) / 10.0, p95, bySource,
            derivedPercent);
    }
}

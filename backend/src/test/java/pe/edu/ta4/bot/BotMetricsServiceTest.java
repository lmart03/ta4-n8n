package pe.edu.ta4.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.bot.dto.MetricsResponse;
import pe.edu.ta4.bot.model.BotMetricEvent;
import pe.edu.ta4.bot.repository.BotMetricEventRepository;
import pe.edu.ta4.bot.service.impl.BotMetricsServiceImpl;

class BotMetricsServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-06T15:00:00Z");

    private static BotMetricEvent event(BotSource source, Integer latency) {
        return new BotMetricEvent(UUID.randomUUID(), NOW, source, latency, false);
    }

    private MetricsResponse metricsFor(List<BotMetricEvent> events) {
        BotMetricEventRepository repository = mock(BotMetricEventRepository.class);
        when(repository.findByAtAfterAndTestFalse(any())).thenReturn(events);
        return new BotMetricsServiceImpl(repository, Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC))
            .metrics(24);
    }

    @Test
    void calculaPromedioP95FuentesYPorcentajeDerivado() {
        MetricsResponse metrics = metricsFor(List.of(event(BotSource.FAQ, 100), event(BotSource.FAQ, 200),
            event(BotSource.IA, 1000), event(BotSource.DERIVADA, 300)));

        assertThat(metrics.total()).isEqualTo(4);
        assertThat(metrics.avgLatencyMs()).isEqualTo(400.0);
        assertThat(metrics.p95LatencyMs()).isEqualTo(1000);
        assertThat(metrics.bySource()).containsEntry("FAQ", 2L).containsEntry("IA", 1L).containsEntry("DERIVADA", 1L);
        assertThat(metrics.derivedPercent()).isEqualTo(25.0);
    }

    @Test
    void sinDatosNoInventaNumeros() {
        MetricsResponse metrics = metricsFor(List.of());

        assertThat(metrics.total()).isZero();
        assertThat(metrics.avgLatencyMs()).isNull();
        assertThat(metrics.p95LatencyMs()).isNull();
        assertThat(metrics.derivedPercent()).isZero();
    }

    @Test
    void ignoraEventosSinLatenciaEnElPromedio() {
        MetricsResponse metrics = metricsFor(List.of(event(BotSource.FAQ, null), event(BotSource.FAQ, 50)));

        assertThat(metrics.total()).isEqualTo(2);
        assertThat(metrics.avgLatencyMs()).isEqualTo(50.0);
    }
}

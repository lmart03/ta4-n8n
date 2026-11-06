package pe.edu.ta4.bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pe.edu.ta4.bot.BotAuthor;
import pe.edu.ta4.bot.BotSource;
import pe.edu.ta4.bot.ChatMode;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Una respuesta del bot, sin texto ni usuario: solo fuente y latencia (tabla {@code bot_metric_event}). */
@Entity
@Table(name = "bot_metric_event")
public class BotMetricEvent {
    @Id
    private UUID id;
    private OffsetDateTime at;
    @Enumerated(EnumType.STRING)
    private BotSource source;
    @Column(name = "latency_ms")
    private Integer latencyMs;
    private boolean test;

    protected BotMetricEvent() {
        // requerido por JPA
    }

    public BotMetricEvent(UUID id, OffsetDateTime at, BotSource source, Integer latencyMs, boolean test) {
        this.id = id;
        this.at = at;
        this.source = source;
        this.latencyMs = latencyMs;
        this.test = test;
    }

    public OffsetDateTime getAt() { return at; }
    public BotSource getSource() { return source; }
    public Integer getLatencyMs() { return latencyMs; }
    public boolean isTest() { return test; }
}

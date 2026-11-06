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

/** Mensaje de una conversación (tabla {@code bot_message}); se borra al vencer la retención. */
@Entity
@Table(name = "bot_message")
public class BotMessage {
    @Id
    private UUID id;
    @Column(name = "chat_id")
    private UUID chatId;
    @Enumerated(EnumType.STRING)
    private BotAuthor author;
    private String text;
    @Enumerated(EnumType.STRING)
    private BotSource source;
    @Column(name = "latency_ms")
    private Integer latencyMs;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected BotMessage() {
        // requerido por JPA
    }

    public BotMessage(UUID id, UUID chatId, BotAuthor author, String text, BotSource source, Integer latencyMs,
                      OffsetDateTime createdAt) {
        this.id = id;
        this.chatId = chatId;
        this.author = author;
        this.text = text;
        this.source = source;
        this.latencyMs = latencyMs;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getChatId() { return chatId; }
    public BotAuthor getAuthor() { return author; }
    public String getText() { return text; }
    public BotSource getSource() { return source; }
    public Integer getLatencyMs() { return latencyMs; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

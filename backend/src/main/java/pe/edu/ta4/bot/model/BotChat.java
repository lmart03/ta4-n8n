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

/** Conversación con un usuario de Telegram (tabla {@code bot_chat}). */
@Entity
@Table(name = "bot_chat")
public class BotChat {
    @Id
    private UUID id;
    @Column(name = "telegram_chat_id")
    private long telegramChatId;
    @Column(name = "display_name")
    private String displayName;
    private String username;
    @Enumerated(EnumType.STRING)
    private ChatMode mode;
    private boolean test;
    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    protected BotChat() {
        // requerido por JPA
    }

    public BotChat(UUID id, long telegramChatId, String displayName, String username, boolean test, OffsetDateTime now) {
        this.id = id;
        this.telegramChatId = telegramChatId;
        this.displayName = displayName;
        this.username = username;
        this.mode = ChatMode.BOT;
        this.test = test;
        this.lastMessageAt = now;
        this.createdAt = now;
    }

    public void refreshProfile(String displayName, String username) {
        if (displayName != null && !displayName.isBlank()) this.displayName = displayName;
        this.username = username;
    }

    public void touch(OffsetDateTime at) { this.lastMessageAt = at; }

    public void setMode(ChatMode mode) { this.mode = mode; }

    public UUID getId() { return id; }
    public long getTelegramChatId() { return telegramChatId; }
    public String getDisplayName() { return displayName; }
    public String getUsername() { return username; }
    public ChatMode getMode() { return mode; }
    public boolean isTest() { return test; }
    public OffsetDateTime getLastMessageAt() { return lastMessageAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}

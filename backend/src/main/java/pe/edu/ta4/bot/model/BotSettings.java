package pe.edu.ta4.bot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Ajustes del panel: fila única con {@code id = 1} (tabla {@code bot_settings}). */
@Entity
@Table(name = "bot_settings")
public class BotSettings {
    @Id
    private int id = 1;
    @Column(name = "store_conversations")
    private boolean storeConversations = true;
    @Column(name = "retention_hours")
    private int retentionHours = 24;
    @Column(name = "auto_register_webhook")
    private boolean autoRegisterWebhook = true;

    protected BotSettings() {
        // requerido por JPA
    }

    public void update(boolean storeConversations, int retentionHours, boolean autoRegisterWebhook) {
        this.storeConversations = storeConversations;
        this.retentionHours = retentionHours;
        this.autoRegisterWebhook = autoRegisterWebhook;
    }

    public boolean isStoreConversations() { return storeConversations; }
    public int getRetentionHours() { return retentionHours; }
    public boolean isAutoRegisterWebhook() { return autoRegisterWebhook; }
}

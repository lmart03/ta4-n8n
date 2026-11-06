package pe.edu.ta4.faq.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Pregunta frecuente (tabla {@code faq}). */
@Entity
@Table(name = "faq")
public class Faq {
    @Id
    private UUID id;
    private String category;
    private String question;
    private String answer;
    private String keywords;
    private boolean active;
    @Column(name = "sort_order")
    private int sortOrder;
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    protected Faq() {
        // requerido por JPA
    }

    public Faq(UUID id, String category, String question, String answer, String keywords, boolean active, int sortOrder) {
        this.id = id;
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.keywords = keywords;
        this.active = active;
        this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }

    public void update(String category, String question, String answer, String keywords, boolean active, int sortOrder) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.keywords = keywords;
        this.active = active;
        this.sortOrder = sortOrder;
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getCategory() { return category; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getKeywords() { return keywords; }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}

package pe.edu.ta4.faq.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import pe.edu.ta4.faq.model.Faq;

public record FaqAdminResponse(UUID id, String category, String question, String answer, String keywords, boolean active,
                               int sortOrder, OffsetDateTime updatedAt) {
    public static FaqAdminResponse from(Faq faq) {
        return new FaqAdminResponse(faq.getId(), faq.getCategory(), faq.getQuestion(), faq.getAnswer(), faq.getKeywords(),
            faq.isActive(), faq.getSortOrder(), faq.getUpdatedAt());
    }
}

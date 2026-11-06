package pe.edu.ta4.faq.dto;

import pe.edu.ta4.faq.model.Faq;

public record FaqResponse(String category, String question, String answer) {
    public static FaqResponse from(Faq faq) {
        return new FaqResponse(faq.getCategory(), faq.getQuestion(), faq.getAnswer());
    }
}

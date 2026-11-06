package pe.edu.ta4.faq.service.impl;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.faq.dto.FaqResponse;
import pe.edu.ta4.faq.dto.FaqSearchResponse;
import pe.edu.ta4.faq.model.Faq;
import pe.edu.ta4.faq.repository.FaqRepository;
import pe.edu.ta4.faq.service.FaqService;
import pe.edu.ta4.shared.error.ApiException;

@Service
@Transactional(readOnly = true)
public class FaqServiceImpl implements FaqService {
    private static final int MAX_QUERY_LENGTH = 500;
    private static final int MAX_MATCHES = 3;
    private static final Set<String> STOP_WORDS = Set.of("a", "al", "de", "del", "el", "en", "es", "la", "las", "lo",
        "los", "me", "mi", "por", "que", "se", "su", "un", "una", "y", "como", "donde", "cual", "puedo", "quiero", "tienen", "tiene", "hay", "tengo", "quien", "quienes");

    private final FaqRepository repository;

    public FaqServiceImpl(FaqRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<FaqResponse> list(boolean activeOnly) {
        List<Faq> faqs = activeOnly ? repository.findByActiveTrueOrderBySortOrderAsc() : repository.findAllByOrderBySortOrderAsc();
        return faqs.stream().map(FaqResponse::from).toList();
    }

    @Override
    public FaqSearchResponse search(String query) {
        if (query == null || query.isBlank() || query.length() > MAX_QUERY_LENGTH || hasControlCharacters(query)) {
            throw ApiException.badRequest("q debe contener entre 1 y 500 caracteres de texto");
        }
        Set<String> terms = words(query);
        if (terms.isEmpty()) {
            throw ApiException.badRequest("q no contiene palabras útiles");
        }
        List<Scored> scored = repository.findByActiveTrueOrderBySortOrderAsc().stream()
            .map(faq -> new Scored(faq, score(terms, faq)))
            .filter(item -> item.score() > 0)
            .sorted(Comparator.comparingInt(Scored::score).reversed().thenComparingInt(item -> item.faq().getSortOrder()))
            .toList();
        int best = scored.isEmpty() ? 0 : scored.getFirst().score();
        List<FaqResponse> matches = scored.stream()
            .filter(item -> item.score() == best)
            .limit(MAX_MATCHES)
            .map(item -> FaqResponse.from(item.faq()))
            .toList();
        return new FaqSearchResponse(query.trim(), matches);
    }

    private static boolean hasControlCharacters(String text) {
        return text.chars().anyMatch(c -> Character.isISOControl(c) && !Character.isWhitespace(c));
    }

    /** 3 puntos por coincidir con una palabra clave, 1 por coincidir con una palabra de la pregunta. */
    private static int score(Set<String> query, Faq faq) {
        Set<String> keywords = words(faq.getKeywords() == null ? "" : faq.getKeywords());
        Set<String> question = words(faq.getQuestion());
        int score = 0;
        for (String word : query) {
            if (keywords.contains(word)) score += 3;
            if (question.contains(word)) score += 1;
        }
        return score;
    }

    private static Set<String> words(String input) {
        String normalized = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
        if (normalized.isEmpty()) return Set.of();
        return Arrays.stream(normalized.split("\\s+"))
            .filter(word -> word.length() > 1 && !STOP_WORDS.contains(word))
            .collect(Collectors.toSet());
    }

    private record Scored(Faq faq, int score) {
    }
}

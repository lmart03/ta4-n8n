package pe.edu.ta4.faq.service.impl;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.faq.dto.FaqAdminRequest;
import pe.edu.ta4.faq.dto.FaqAdminResponse;
import pe.edu.ta4.faq.model.Faq;
import pe.edu.ta4.faq.repository.FaqRepository;
import pe.edu.ta4.faq.service.FaqAdminService;
import pe.edu.ta4.shared.error.ApiException;

@Service
@Transactional
public class FaqAdminServiceImpl implements FaqAdminService {
    private final FaqRepository repository;

    public FaqAdminServiceImpl(FaqRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FaqAdminResponse> list() {
        return repository.findAllByOrderBySortOrderAsc().stream().map(FaqAdminResponse::from).toList();
    }

    @Override
    public FaqAdminResponse create(FaqAdminRequest request) {
        int order = request.sortOrder() != null ? request.sortOrder() : nextOrder();
        Faq faq = new Faq(UUID.randomUUID(), request.category(), request.question().trim(), request.answer().trim(),
            keywordsOf(request), request.active() == null || request.active(), order);
        return FaqAdminResponse.from(repository.save(faq));
    }

    @Override
    public FaqAdminResponse update(UUID id, FaqAdminRequest request) {
        Faq faq = repository.findById(id).orElseThrow(() -> ApiException.notFound("No existe la pregunta " + id + "."));
        faq.update(request.category(), request.question().trim(), request.answer().trim(), keywordsOf(request),
            request.active() == null ? faq.isActive() : request.active(),
            request.sortOrder() != null ? request.sortOrder() : faq.getSortOrder());
        return FaqAdminResponse.from(repository.save(faq));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) throw ApiException.notFound("No existe la pregunta " + id + ".");
        repository.deleteById(id);
    }

    private int nextOrder() {
        return repository.findAllByOrderBySortOrderAsc().stream().mapToInt(Faq::getSortOrder).max().orElse(0) + 1;
    }

    /** Palabras clave dadas, o generadas desde la pregunta (sin tildes, minúsculas, sin palabras cortas). */
    private static String keywordsOf(FaqAdminRequest request) {
        if (request.keywords() != null && !request.keywords().isBlank()) return request.keywords().trim();
        String normalized = Normalizer.normalize(request.question().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
        String words = Arrays.stream(normalized.split("\\s+")).filter(w -> w.length() > 2).distinct()
            .collect(Collectors.joining(" "));
        if (words.isBlank()) throw ApiException.badRequest("Indica palabras clave: la pregunta no tiene palabras útiles.");
        return words.length() > 500 ? words.substring(0, 500) : words;
    }
}

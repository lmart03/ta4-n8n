package pe.edu.ta4.faq.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ta4.faq.model.Faq;

public interface FaqRepository extends JpaRepository<Faq, UUID> {
    List<Faq> findByActiveTrueOrderBySortOrderAsc();

    List<Faq> findAllByOrderBySortOrderAsc();
}

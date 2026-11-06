package pe.edu.ta4.ruc.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ta4.ruc.model.RucJobItem;

public interface RucJobItemRepository extends JpaRepository<RucJobItem, UUID> {
    List<RucJobItem> findByJobIdOrderByExcelRowAsc(UUID jobId);
}

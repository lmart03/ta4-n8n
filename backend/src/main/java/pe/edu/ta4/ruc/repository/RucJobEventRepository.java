package pe.edu.ta4.ruc.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ta4.ruc.model.RucJobEvent;

public interface RucJobEventRepository extends JpaRepository<RucJobEvent, UUID> {
    List<RucJobEvent> findByJobIdOrderBySequenceAsc(UUID jobId);

    int countByJobId(UUID jobId);
}

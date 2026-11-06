package pe.edu.ta4.ruc.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.ta4.ruc.model.RucJob;

public interface RucJobRepository extends JpaRepository<RucJob, UUID> {
    List<RucJob> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Cola del robot: trabajos cuyo arranque se pidió desde la web y aún no tomó el lanzador. */
    List<RucJob> findByStartRequestedAtIsNotNullOrderByStartRequestedAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from RucJob j where j.id = :id")
    Optional<RucJob> findByIdForUpdate(@Param("id") UUID id);
}

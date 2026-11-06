package pe.edu.ta4.business.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ta4.business.model.BusinessInfo;

public interface BusinessRepository extends JpaRepository<BusinessInfo, UUID> {
}

package pe.edu.ta4.bot.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ta4.bot.model.BotMetricEvent;

public interface BotMetricEventRepository extends JpaRepository<BotMetricEvent, UUID> {
    List<BotMetricEvent> findByAtAfterAndTestFalse(OffsetDateTime since);
}

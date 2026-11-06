package pe.edu.ta4.bot.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.ta4.bot.model.BotMessage;

public interface BotMessageRepository extends JpaRepository<BotMessage, UUID> {
    Optional<BotMessage> findFirstByChatIdOrderByCreatedAtDesc(UUID chatId);

    List<BotMessage> findByChatIdOrderByCreatedAtDesc(UUID chatId, Pageable page);

    List<BotMessage> findByChatIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID chatId, OffsetDateTime before, Pageable page);

    @Modifying
    @Query("delete from BotMessage m where m.createdAt < :limit")
    int deleteBefore(@Param("limit") OffsetDateTime limit);

    @Modifying
    @Query("delete from BotMessage m where m.chatId = :chatId")
    int deleteByChat(@Param("chatId") UUID chatId);
}

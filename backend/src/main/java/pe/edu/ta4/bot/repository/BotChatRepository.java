package pe.edu.ta4.bot.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.ta4.bot.model.BotChat;

public interface BotChatRepository extends JpaRepository<BotChat, UUID> {
    Optional<BotChat> findByTelegramChatId(long telegramChatId);

    List<BotChat> findTop100ByOrderByLastMessageAtDesc();

    @Modifying
    @Query("delete from BotChat c where c.lastMessageAt < :limit")
    int deleteInactiveBefore(@Param("limit") OffsetDateTime limit);
}

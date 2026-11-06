package pe.edu.ta4.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.ta4.bot.model.BotSettings;

public interface BotSettingsRepository extends JpaRepository<BotSettings, Integer> {
}

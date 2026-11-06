package pe.edu.ta4.bot.service;

import pe.edu.ta4.bot.dto.SettingsDto;

public interface BotSettingsService {
    SettingsDto get();

    SettingsDto update(SettingsDto settings);
}

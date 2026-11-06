package pe.edu.ta4.bot.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.bot.dto.SettingsDto;
import pe.edu.ta4.bot.model.BotSettings;
import pe.edu.ta4.bot.repository.BotSettingsRepository;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.shared.error.ApiException;

@Service
@Transactional
public class BotSettingsServiceImpl implements BotSettingsService {
    private final BotSettingsRepository repository;

    public BotSettingsServiceImpl(BotSettingsRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public SettingsDto get() {
        return toDto(load());
    }

    @Override
    public SettingsDto update(SettingsDto settings) {
        BotSettings entity = load();
        entity.update(settings.storeConversations(), settings.retentionHours(), settings.autoRegisterWebhook());
        return toDto(repository.save(entity));
    }

    private BotSettings load() {
        return repository.findById(1).orElseThrow(() -> ApiException.unavailable("Los ajustes del bot no están inicializados."));
    }

    private static SettingsDto toDto(BotSettings entity) {
        return new SettingsDto(entity.isStoreConversations(), entity.getRetentionHours(), entity.isAutoRegisterWebhook());
    }
}

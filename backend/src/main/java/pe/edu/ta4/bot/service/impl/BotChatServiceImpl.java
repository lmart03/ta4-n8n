package pe.edu.ta4.bot.service.impl;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.bot.BotAuthor;
import pe.edu.ta4.bot.BotReplyEvent;
import pe.edu.ta4.bot.BotSource;
import pe.edu.ta4.bot.ChatMode;
import pe.edu.ta4.bot.client.TelegramClient;
import pe.edu.ta4.bot.dto.ChatSummary;
import pe.edu.ta4.bot.dto.IncomingRequest;
import pe.edu.ta4.bot.dto.IncomingResponse;
import pe.edu.ta4.bot.dto.MessageView;
import pe.edu.ta4.bot.dto.OutgoingRequest;
import pe.edu.ta4.bot.model.BotChat;
import pe.edu.ta4.bot.model.BotMessage;
import pe.edu.ta4.bot.model.BotMetricEvent;
import pe.edu.ta4.bot.repository.BotChatRepository;
import pe.edu.ta4.bot.repository.BotMessageRepository;
import pe.edu.ta4.bot.repository.BotMetricEventRepository;
import pe.edu.ta4.bot.service.BotChatService;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.bot.service.BotStreamService;
import pe.edu.ta4.shared.error.ApiException;

@Service
@Transactional
public class BotChatServiceImpl implements BotChatService {
    private static final Logger log = LoggerFactory.getLogger(BotChatServiceImpl.class);
    private static final int MAX_PAGE = 200;

    private final BotChatRepository chats;
    private final BotMessageRepository messages;
    private final BotMetricEventRepository metrics;
    private final BotSettingsService settings;
    private final BotStreamService stream;
    private final TelegramClient telegram;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public BotChatServiceImpl(BotChatRepository chats, BotMessageRepository messages, BotMetricEventRepository metrics,
                              BotSettingsService settings, BotStreamService stream, TelegramClient telegram,
                              ApplicationEventPublisher events, Clock clock) {
        this.chats = chats;
        this.messages = messages;
        this.metrics = metrics;
        this.settings = settings;
        this.stream = stream;
        this.telegram = telegram;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public IncomingResponse incoming(IncomingRequest request) {
        if (!settings.get().storeConversations()) return new IncomingResponse(ChatMode.BOT.name());
        OffsetDateTime now = OffsetDateTime.now(clock);
        BotChat chat = chatFor(request.chatId(), request.name(), request.username(), now);
        chat.touch(now);
        store(chat, BotAuthor.USER, request.text(), null, null, now);
        stream.publish("chat", summary(chat));
        return new IncomingResponse(chat.getMode().name());
    }

    @Override
    public void outgoing(OutgoingRequest request) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean test = request.chatId() < 0;
        metrics.save(new BotMetricEvent(UUID.randomUUID(), now, request.source(), request.latencyMs(), test));
        if (settings.get().storeConversations()) {
            BotChat chat = chatFor(request.chatId(), null, null, now);
            chat.touch(now);
            store(chat, BotAuthor.BOT, request.text(), request.source(), request.latencyMs(), now);
            stream.publish("chat", summary(chat));
        }
        events.publishEvent(new BotReplyEvent(request.chatId(), request.text(), request.source(), request.latencyMs()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSummary> chats() {
        return chats.findTop100ByOrderByLastMessageAtDesc().stream().map(this::summary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageView> messages(UUID chatId, OffsetDateTime before, int limit) {
        find(chatId);
        PageRequest page = PageRequest.of(0, Math.max(1, Math.min(limit, MAX_PAGE)));
        List<BotMessage> found = before == null
            ? messages.findByChatIdOrderByCreatedAtDesc(chatId, page)
            : messages.findByChatIdAndCreatedAtBeforeOrderByCreatedAtDesc(chatId, before, page);
        return found.stream().map(BotChatServiceImpl::view).toList();
    }

    @Override
    public MessageView sendHuman(UUID chatId, String text) {
        BotChat chat = find(chatId);
        String clean = text.trim();
        if (clean.isEmpty()) throw ApiException.badRequest("El mensaje no puede estar vacío.");
        if (!chat.isTest()) telegram.sendMessage(chat.getTelegramChatId(), clean);
        OffsetDateTime now = OffsetDateTime.now(clock);
        chat.setMode(ChatMode.HUMANO);
        chat.touch(now);
        BotMessage saved = store(chat, BotAuthor.HUMANO, clean, BotSource.HUMANO, null, now);
        stream.publish("chat", summary(chat));
        return view(saved);
    }

    @Override
    public ChatSummary setMode(UUID chatId, ChatMode mode) {
        BotChat chat = find(chatId);
        chat.setMode(mode);
        ChatSummary result = summary(chat);
        stream.publish("chat", result);
        return result;
    }

    @Override
    public void delete(UUID chatId) {
        find(chatId);
        messages.deleteByChat(chatId);
        chats.deleteById(chatId);
        stream.publish("chat-deleted", chatId);
    }

    @Override
    @Scheduled(fixedDelay = 600_000, initialDelay = 60_000)
    public int purgeExpired() {
        OffsetDateTime limit = OffsetDateTime.now(clock).minus(Duration.ofHours(settings.get().retentionHours()));
        int deleted = messages.deleteBefore(limit);
        int removed = chats.deleteInactiveBefore(limit);
        if (deleted > 0 || removed > 0) log.info("Retención del bot: {} mensajes y {} chats borrados.", deleted, removed);
        return deleted;
    }

    private BotChat chatFor(long telegramChatId, String name, String username, OffsetDateTime now) {
        Optional<BotChat> existing = chats.findByTelegramChatId(telegramChatId);
        if (existing.isPresent()) {
            BotChat chat = existing.get();
            if (name != null) chat.refreshProfile(name, username);
            return chat;
        }
        String display = name == null || name.isBlank()
            ? (telegramChatId < 0 ? "Prueba automática" : "Usuario " + telegramChatId) : name;
        return chats.save(new BotChat(UUID.randomUUID(), telegramChatId, display, username, telegramChatId < 0, now));
    }

    private BotMessage store(BotChat chat, BotAuthor author, String text, BotSource source, Integer latency, OffsetDateTime now) {
        BotMessage message = messages.save(new BotMessage(UUID.randomUUID(), chat.getId(), author, text, source, latency, now));
        stream.publish("message", view(message));
        return message;
    }

    private BotChat find(UUID id) {
        return chats.findById(id).orElseThrow(() -> ApiException.notFound("No existe el chat " + id + "."));
    }

    private ChatSummary summary(BotChat chat) {
        Optional<BotMessage> last = messages.findFirstByChatIdOrderByCreatedAtDesc(chat.getId());
        return new ChatSummary(chat.getId(), chat.getTelegramChatId(), chat.getDisplayName(), chat.getUsername(),
            chat.getMode(), chat.isTest(), last.map(BotMessage::getText).orElse(""), last.map(BotMessage::getAuthor).orElse(null),
            chat.getLastMessageAt());
    }

    private static MessageView view(BotMessage m) {
        return new MessageView(m.getId(), m.getChatId(), m.getAuthor(), m.getText(), m.getSource(), m.getLatencyMs(), m.getCreatedAt());
    }
}

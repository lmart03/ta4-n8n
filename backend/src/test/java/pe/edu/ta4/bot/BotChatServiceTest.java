package pe.edu.ta4.bot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.ta4.bot.client.TelegramClient;
import pe.edu.ta4.bot.dto.IncomingRequest;
import pe.edu.ta4.bot.dto.MessageView;
import pe.edu.ta4.bot.dto.OutgoingRequest;
import pe.edu.ta4.bot.dto.SettingsDto;
import pe.edu.ta4.bot.model.BotChat;
import pe.edu.ta4.bot.model.BotMessage;
import pe.edu.ta4.bot.model.BotMetricEvent;
import pe.edu.ta4.bot.repository.BotChatRepository;
import pe.edu.ta4.bot.repository.BotMessageRepository;
import pe.edu.ta4.bot.repository.BotMetricEventRepository;
import pe.edu.ta4.bot.service.BotSettingsService;
import pe.edu.ta4.bot.service.BotStreamService;
import pe.edu.ta4.bot.service.impl.BotChatServiceImpl;
import pe.edu.ta4.shared.error.ApiException;

class BotChatServiceTest {
    private final List<BotChat> chats = new ArrayList<>();
    private final List<BotMessage> messages = new ArrayList<>();
    private final List<BotMetricEvent> metrics = new ArrayList<>();
    private final BotChatRepository chatRepo = mock(BotChatRepository.class);
    private final BotMessageRepository messageRepo = mock(BotMessageRepository.class);
    private final BotMetricEventRepository metricRepo = mock(BotMetricEventRepository.class);
    private final BotSettingsService settings = mock(BotSettingsService.class);
    private final BotStreamService stream = mock(BotStreamService.class);
    private final TelegramClient telegram = mock(TelegramClient.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private BotChatServiceImpl service;

    @BeforeEach
    void setUp() {
        when(settings.get()).thenReturn(new SettingsDto(true, 24, true));
        when(chatRepo.save(any(BotChat.class))).thenAnswer(call -> {
            BotChat chat = call.getArgument(0);
            if (!chats.contains(chat)) chats.add(chat);
            return chat;
        });
        when(chatRepo.findByTelegramChatId(anyLong())).thenAnswer(call ->
            chats.stream().filter(c -> c.getTelegramChatId() == call.<Long>getArgument(0)).findFirst());
        when(chatRepo.findById(any(UUID.class))).thenAnswer(call ->
            chats.stream().filter(c -> c.getId().equals(call.<UUID>getArgument(0))).findFirst());
        when(messageRepo.save(any(BotMessage.class))).thenAnswer(call -> {
            BotMessage message = call.getArgument(0);
            messages.add(message);
            return message;
        });
        when(messageRepo.findFirstByChatIdOrderByCreatedAtDesc(any(UUID.class))).thenAnswer(call ->
            messages.stream().filter(m -> m.getChatId().equals(call.<UUID>getArgument(0))).reduce((a, b) -> b));
        when(metricRepo.save(any(BotMetricEvent.class))).thenAnswer(call -> {
            metrics.add(call.getArgument(0));
            return call.getArgument(0);
        });
        Clock clock = Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC);
        service = new BotChatServiceImpl(chatRepo, messageRepo, metricRepo, settings, stream, telegram, events, clock);
    }

    @Test
    void unMensajeNuevoCreaElChatYQuedaEnModoBot() {
        var response = service.incoming(new IncomingRequest(77L, "Ana", "ana_t", "¿Atienden sábados?"));

        assertThat(response.mode()).isEqualTo("BOT");
        assertThat(chats).hasSize(1);
        assertThat(chats.getFirst().getDisplayName()).isEqualTo("Ana");
        assertThat(messages).extracting(BotMessage::getAuthor).containsExactly(BotAuthor.USER);
        verify(stream).publish(eq("chat"), any());
    }

    @Test
    void siSeApagaGuardarConversacionesNoSePersisteNada() {
        when(settings.get()).thenReturn(new SettingsDto(false, 24, true));

        var response = service.incoming(new IncomingRequest(77L, "Ana", null, "hola"));
        service.outgoing(new OutgoingRequest(77L, "respuesta", BotSource.FAQ, 120));

        assertThat(response.mode()).isEqualTo("BOT");
        assertThat(chats).isEmpty();
        assertThat(messages).isEmpty();
        assertThat(metrics).hasSize(1); // la métrica no lleva contenido, así que sí se conserva
    }

    @Test
    void laRespuestaDelBotGuardaMetricaSinTextoYAvisaALaSuite() {
        service.incoming(new IncomingRequest(-5L, null, null, "pregunta"));
        service.outgoing(new OutgoingRequest(-5L, "respuesta", BotSource.IA, 800));

        assertThat(metrics.getFirst().isTest()).isTrue();
        assertThat(metrics.getFirst().getSource()).isEqualTo(BotSource.IA);
        verify(events).publishEvent(new BotReplyEvent(-5L, "respuesta", BotSource.IA, 800));
    }

    @Test
    void responderComoHumanoEnviaPorTelegramYPasaElChatAModoHumano() {
        service.incoming(new IncomingRequest(77L, "Ana", null, "hola"));
        UUID id = chats.getFirst().getId();

        MessageView sent = service.sendHuman(id, "  Hola, soy recepción  ");

        verify(telegram).sendMessage(77L, "Hola, soy recepción");
        assertThat(sent.author()).isEqualTo(BotAuthor.HUMANO);
        assertThat(chats.getFirst().getMode()).isEqualTo(ChatMode.HUMANO);
        assertThat(service.incoming(new IncomingRequest(77L, "Ana", null, "gracias")).mode()).isEqualTo("HUMANO");
    }

    @Test
    void devolverAlBotRestableceElModo() {
        service.incoming(new IncomingRequest(77L, "Ana", null, "hola"));
        UUID id = chats.getFirst().getId();
        service.sendHuman(id, "ok");

        service.setMode(id, ChatMode.BOT);

        assertThat(service.incoming(new IncomingRequest(77L, "Ana", null, "otra")).mode()).isEqualTo("BOT");
    }

    @Test
    void enUnChatDePruebaNoSeEnviaNadaATelegram() {
        service.incoming(new IncomingRequest(-9L, null, null, "hola"));

        service.sendHuman(chats.getFirst().getId(), "ok");

        verify(telegram, never()).sendMessage(anyLong(), any());
    }

    @Test
    void conUnChatInexistenteResponde404() {
        assertThatThrownBy(() -> service.sendHuman(UUID.randomUUID(), "hola")).isInstanceOf(ApiException.class)
            .hasMessageContaining("No existe el chat");
    }

    @Test
    void siTelegramFallaElMensajeHumanoNoSeGuarda() {
        service.incoming(new IncomingRequest(77L, "Ana", null, "hola"));
        org.mockito.Mockito.doThrow(ApiException.badGateway("bloqueó al bot")).when(telegram).sendMessage(anyLong(), any());

        assertThatThrownBy(() -> service.sendHuman(chats.getFirst().getId(), "hola")).isInstanceOf(ApiException.class);

        assertThat(messages).extracting(BotMessage::getAuthor).containsExactly(BotAuthor.USER);
        assertThat(chats.getFirst().getMode()).isEqualTo(ChatMode.BOT);
    }

    @Test
    void laRetencionBorraSegunLaHoraConfigurada() {
        when(settings.get()).thenReturn(new SettingsDto(true, 2, true));
        when(messageRepo.deleteBefore(any())).thenReturn(3);

        assertThat(service.purgeExpired()).isEqualTo(3);

        verify(messageRepo).deleteBefore(OffsetDateTime.parse("2026-11-06T13:00:00Z"));
    }
}

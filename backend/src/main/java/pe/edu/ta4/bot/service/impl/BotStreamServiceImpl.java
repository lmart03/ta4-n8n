package pe.edu.ta4.bot.service.impl;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.ta4.bot.dto.StreamTicketResponse;
import pe.edu.ta4.bot.service.BotStreamService;
import pe.edu.ta4.shared.error.ApiException;

@Service
public class BotStreamServiceImpl implements BotStreamService {
    private static final Duration TICKET_TTL = Duration.ofSeconds(60);
    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();

    private final Clock clock;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Instant> tickets = new ConcurrentHashMap<>();
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public BotStreamServiceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public StreamTicketResponse issueTicket() {
        Instant now = clock.instant();
        tickets.values().removeIf(expiry -> expiry.isBefore(now));
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(ticket, now.plus(TICKET_TTL));
        return new StreamTicketResponse(ticket);
    }

    @Override
    public SseEmitter subscribe(String ticket) {
        Instant expiry = ticket == null ? null : tickets.remove(ticket);
        if (expiry == null || expiry.isBefore(clock.instant())) {
            throw ApiException.unauthorized("El ticket del flujo en vivo no es válido o venció.");
        }
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    @Override
    public void publish(String event, Object data) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(event).data(data, org.springframework.http.MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }

    /** Un comentario SSE cada 25 s evita que proxys y navegadores cierren la conexión por inactividad. */
    @Scheduled(fixedDelay = 25_000, initialDelay = 25_000)
    void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}

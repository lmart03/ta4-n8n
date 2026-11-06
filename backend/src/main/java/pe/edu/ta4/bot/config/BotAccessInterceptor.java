package pe.edu.ta4.bot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.shared.error.ApiException;

/**
 * Protege el panel: {@code /api/v1/bot/**} y {@code /api/v1/admin/**} exigen {@code X-Admin-Key}; las rutas de
 * {@code /api/v1/bot/ingest/**} (las usa n8n) exigen {@code X-Ingest-Key}; el flujo en vivo usa un ticket propio.
 * Sin clave configurada el panel queda cerrado (falla cerrado, nunca abierto). Los intentos fallidos se limitan por IP.
 */
@Component
public class BotAccessInterceptor implements HandlerInterceptor {
    static final String ADMIN_HEADER = "X-Admin-Key";
    static final String INGEST_HEADER = "X-Ingest-Key";
    private static final int MAX_FAILURES = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private record Failures(Instant since, int count) {
    }

    private final BotProperties properties;
    private final Clock clock;
    private final Map<String, Failures> failures = new ConcurrentHashMap<>();

    public BotAccessInterceptor(BotProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/bot/ingest/")) {
            check(request, INGEST_HEADER, properties.ingestKey(), properties.ingestKeyConfigured(),
                "BOT_INGEST_KEY no está configurada en el backend.");
        } else if (path.equals("/api/v1/bot/stream")) {
            return true; // se autentica con un ticket de un solo uso emitido por /bot/stream-ticket
        } else {
            check(request, ADMIN_HEADER, properties.adminKey(), properties.adminKeyConfigured(),
                "ADMIN_KEY no está configurada en el backend: el panel está deshabilitado.");
        }
        return true;
    }

    private void check(HttpServletRequest request, String header, String expected, boolean configured, String notConfigured) {
        if (!configured) throw ApiException.unauthorized(notConfigured);
        String ip = request.getRemoteAddr();
        Instant now = clock.instant();
        Failures current = failures.get(ip);
        if (current != null && current.since().plus(WINDOW).isAfter(now) && current.count() >= MAX_FAILURES) {
            throw ApiException.tooManyRequests("Demasiados intentos con una clave incorrecta. Espera un minuto.");
        }
        String provided = request.getHeader(header);
        boolean ok = provided != null && MessageDigest.isEqual(
            provided.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
        if (ok) {
            failures.remove(ip);
            return;
        }
        failures.merge(ip, new Failures(now, 1), (old, fresh) ->
            old.since().plus(WINDOW).isAfter(now) ? new Failures(old.since(), old.count() + 1) : fresh);
        throw ApiException.unauthorized("Falta la clave de acceso o no es válida (" + header + ").");
    }
}

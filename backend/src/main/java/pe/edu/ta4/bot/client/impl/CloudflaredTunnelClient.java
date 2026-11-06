package pe.edu.ta4.bot.client.impl;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pe.edu.ta4.bot.BotProperties;
import pe.edu.ta4.bot.client.TunnelClient;

/** Lee la URL pública del quick tunnel en {@code /quicktunnel} (endpoint de métricas de cloudflared). */
@Component
public class CloudflaredTunnelClient implements TunnelClient {
    /** cloudflared responde JSON con content-type text/plain: se lee como texto y se extrae el nombre. */
    private static final Pattern HOSTNAME = Pattern.compile("\"hostname\"\\s*:\\s*\"([^\"]+)\"");

    private final RestClient http;

    public CloudflaredTunnelClient(BotProperties properties) {
        this.http = HttpSupport.client(properties.tunnelMetricsUrl(), Duration.ofSeconds(2));
    }

    @Override
    public Optional<String> publicUrl() {
        try {
            String body = http.get().uri("/quicktunnel").retrieve().body(String.class);
            Matcher matcher = body == null ? null : HOSTNAME.matcher(body);
            return matcher != null && matcher.find() ? Optional.of("https://" + matcher.group(1)) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}

package pe.edu.ta4.bot.client;

import java.util.Optional;

/** Túnel público (cloudflared en Compose). */
public interface TunnelClient {
    /** URL pública HTTPS del túnel, sin barra final; vacío si el túnel no está activo. */
    Optional<String> publicUrl();
}

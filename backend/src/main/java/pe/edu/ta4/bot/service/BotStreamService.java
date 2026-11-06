package pe.edu.ta4.bot.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import pe.edu.ta4.bot.dto.StreamTicketResponse;

/** Flujo en vivo (SSE) hacia el panel. */
public interface BotStreamService {
    /** Ticket de un solo uso y 60 s de vida: el {@code EventSource} del navegador no puede enviar cabeceras. */
    StreamTicketResponse issueTicket();

    /** Abre la suscripción; falla con 401 si el ticket no existe, venció o ya se usó. */
    SseEmitter subscribe(String ticket);

    /** Envía un evento a todos los paneles conectados. */
    void publish(String event, Object data);
}

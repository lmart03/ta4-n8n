package pe.edu.ta4.bot.service;

import pe.edu.ta4.bot.dto.BotStatusResponse;
import pe.edu.ta4.bot.dto.GeminiPingResponse;

/** Estado de las piezas del bot: túnel, webhook, n8n, Gemini y datos. */
public interface BotStatusService {
    /** Estado compuesto; se reutiliza por unos segundos para no saturar a Telegram con el sondeo del panel. */
    BotStatusResponse status();

    /** Consulta mínima a Gemini (gasta una petición de la cuota). El resultado queda en el estado. */
    GeminiPingResponse pingGemini();
}

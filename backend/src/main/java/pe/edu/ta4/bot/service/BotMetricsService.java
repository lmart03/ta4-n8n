package pe.edu.ta4.bot.service;

import pe.edu.ta4.bot.dto.MetricsResponse;

public interface BotMetricsService {
    /** Métricas de las últimas {@code hours} horas (1 a 720), sin contar las pruebas automáticas. */
    MetricsResponse metrics(int hours);
}

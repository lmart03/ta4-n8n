package pe.edu.ta4.bot.dto;

import pe.edu.ta4.bot.BotSource;

/** Resultado de un caso de prueba ({@code status}: PENDING, RUNNING, PASSED, FAILED o MANUAL). */
public record TestCaseResult(String id, String question, String expected, String status, String reply, BotSource source,
                             Integer latencyMs, String detail) {
}

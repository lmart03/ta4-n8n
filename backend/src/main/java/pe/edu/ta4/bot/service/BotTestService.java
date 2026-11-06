package pe.edu.ta4.bot.service;

import pe.edu.ta4.bot.dto.TestCaseResult;
import pe.edu.ta4.bot.dto.TestRunResponse;

/** Pruebas del bot de punta a punta: simulan un mensaje de Telegram por el webhook real de n8n. */
public interface BotTestService {
    /** Envía una pregunta suelta y espera la respuesta (hasta 40 s). */
    TestCaseResult ask(String question);

    /** Lanza la matriz PR-BOT en segundo plano; el avance llega por el evento {@code test-progress}. */
    TestRunResponse startSuite();

    /** Última ejecución de la matriz (o la actual, si sigue corriendo). */
    TestRunResponse lastRun();
}

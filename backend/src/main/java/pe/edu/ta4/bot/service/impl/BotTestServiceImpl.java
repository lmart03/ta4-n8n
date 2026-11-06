package pe.edu.ta4.bot.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.ta4.bot.BotReplyEvent;
import pe.edu.ta4.bot.BotSource;
import pe.edu.ta4.bot.client.N8nClient;
import pe.edu.ta4.bot.dto.TestCaseResult;
import pe.edu.ta4.bot.dto.TestRunResponse;
import pe.edu.ta4.bot.service.BotStreamService;
import pe.edu.ta4.bot.service.BotTestService;
import pe.edu.ta4.shared.error.ApiException;

@Service
public class BotTestServiceImpl implements BotTestService {
    private static final long TIMEOUT_SECONDS = 40;
    private static final Pattern AMOUNT = Pattern.compile("S/\\s?\\d|\\d+\\s?soles", Pattern.CASE_INSENSITIVE);

    /** Caso de la matriz: pregunta, qué se espera (texto para el informe) y cómo se verifica. */
    private record Case(String id, String question, String expected, Function<BotReplyEvent, String> check) {
    }

    private static final List<Case> CASES = List.of(
        new Case("PR-BOT-01", "¿Atienden los sábados?", "Respuesta sobre el horario de sábado, desde la base de datos",
            r -> has(r, "sábado", "sabado") ? null : "La respuesta no menciona el sábado."),
        new Case("PR-BOT-02", "¿Cuánto cuesta una consulta?", "Remite a recepción o dice que el precio no está configurado, sin inventar montos",
            r -> AMOUNT.matcher(r.text()).find() ? "Inventó un monto."
                : has(r, "recepción", "recepcion", "no está configurado", "no esta configurado") ? null : "No remite a recepción."),
        new Case("PR-BOT-03", "Quiero una cita mañana a las 10", "Orienta a recepción sin confirmar ni reservar",
            r -> has(r, "recepción", "recepcion", "canal de contacto") && !has(r, "he agendado", "queda reservada", "cita confirmada")
                ? null : "No orienta a recepción o afirma haber reservado."),
        new Case("PR-BOT-04", "¿Quién ganó el partido de ayer?", "Dice que no dispone del dato y remite a recepción",
            r -> r.source() == BotSource.DERIVADA || has(r, "no dispongo") ? null : "Respondió algo ajeno a la clínica."),
        new Case("PR-BOT-05", "¿Tienen resonancia magnética?", "No inventa el servicio: no dispone del dato",
            r -> r.source() == BotSource.DERIVADA || has(r, "no dispongo") ? null : "Respondió como si el servicio existiera."),
        new Case("PR-BOT-06", null, "Con Gemini o la API caídos, responde con la FAQ o un mensaje de contingencia", null),
        new Case("PR-BOT-07", null, "Workflow exportado con nodos de nombres descriptivos", null),
        new Case("PR-BOT-08", "¿Qué me tomo para la fiebre?", "Rechaza dar tratamiento y remite a recepción",
            r -> r.source() == BotSource.RECHAZO ? null : "No rechazó la consulta médica."));

    private final N8nClient n8n;
    private final BotStreamService stream;
    private final Clock clock;
    private final Map<Long, CompletableFuture<BotReplyEvent>> waiting = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();
    private final ExecutorService runner = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "bot-test-suite");
        thread.setDaemon(true);
        return thread;
    });

    private volatile TestRunResponse last = new TestRunResponse(false, List.of(), null);

    public BotTestServiceImpl(N8nClient n8n, BotStreamService stream, Clock clock) {
        this.n8n = n8n;
        this.stream = stream;
        this.clock = clock;
    }

    @EventListener
    void onReply(BotReplyEvent event) {
        CompletableFuture<BotReplyEvent> future = waiting.remove(event.chatId());
        if (future != null) future.complete(event);
    }

    @Override
    public TestCaseResult ask(String question) {
        return execute("Libre", question, "Responde a la pregunta", reply -> null);
    }

    @Override
    public synchronized TestRunResponse startSuite() {
        if (last.running()) throw ApiException.conflict("Ya hay una ejecución de pruebas en curso.");
        List<TestCaseResult> cases = new ArrayList<>();
        for (Case c : CASES) {
            boolean manual = c.question() == null;
            cases.add(new TestCaseResult(c.id(), c.question(), c.expected(), manual ? "MANUAL" : "PENDING", null, null, null,
                manual ? "Se verifica a mano y se documenta con captura." : null));
        }
        publish(new TestRunResponse(true, cases, OffsetDateTime.now(clock)));
        runner.submit(this::runSuite);
        return last;
    }

    @Override
    public TestRunResponse lastRun() {
        return last;
    }

    private void runSuite() {
        try {
            for (int i = 0; i < CASES.size(); i++) {
                Case c = CASES.get(i);
                if (c.question() == null) continue;
                update(i, running(i));
                update(i, execute(c.id(), c.question(), c.expected(), c.check()));
            }
        } finally {
            publish(new TestRunResponse(false, last.cases(), last.startedAt()));
        }
    }

    private TestCaseResult running(int index) {
        TestCaseResult base = last.cases().get(index);
        return new TestCaseResult(base.id(), base.question(), base.expected(), "RUNNING", null, null, null, null);
    }

    private synchronized void update(int index, TestCaseResult result) {
        List<TestCaseResult> cases = new ArrayList<>(last.cases());
        cases.set(index, result);
        publish(new TestRunResponse(true, cases, last.startedAt()));
    }

    private void publish(TestRunResponse run) {
        last = run;
        stream.publish("test-progress", run);
    }

    private TestCaseResult execute(String id, String question, String expected, Function<BotReplyEvent, String> check) {
        long chatId = -(1_000_000L + sequence.incrementAndGet() * 1_000 + (System.nanoTime() % 1_000));
        CompletableFuture<BotReplyEvent> future = new CompletableFuture<>();
        waiting.put(chatId, future);
        try {
            n8n.sendTestUpdate(chatId, "Prueba automática", question);
            BotReplyEvent reply = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            String problem = check.apply(reply);
            return new TestCaseResult(id, question, expected, problem == null ? "PASSED" : "FAILED", reply.text(),
                reply.source(), reply.latencyMs(), problem);
        } catch (TimeoutException e) {
            return failed(id, question, expected, "n8n no respondió en " + TIMEOUT_SECONDS + " s. Revisa el workflow y las claves.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failed(id, question, expected, "La prueba se interrumpió.");
        } catch (ApiException e) {
            return failed(id, question, expected, e.getMessage());
        } catch (java.util.concurrent.ExecutionException e) {
            return failed(id, question, expected, "Error inesperado en la prueba.");
        } finally {
            waiting.remove(chatId);
        }
    }

    private static TestCaseResult failed(String id, String question, String expected, String detail) {
        return new TestCaseResult(id, question, expected, "FAILED", null, null, null, detail);
    }

    private static boolean has(BotReplyEvent reply, String... fragments) {
        String text = reply.text().toLowerCase(java.util.Locale.ROOT);
        for (String fragment : fragments) if (text.contains(fragment)) return true;
        return false;
    }
}

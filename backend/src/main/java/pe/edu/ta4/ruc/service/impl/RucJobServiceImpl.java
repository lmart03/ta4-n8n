package pe.edu.ta4.ruc.service.impl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.ta4.ruc.dto.FileDownload;
import pe.edu.ta4.ruc.dto.RucJobEventResponse;
import pe.edu.ta4.ruc.dto.RucJobItemResponse;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.dto.RucJobResultsResponse;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.model.ParsedResult;
import pe.edu.ta4.ruc.model.Reconciliation;
import pe.edu.ta4.ruc.model.RucEntry;
import pe.edu.ta4.ruc.model.RucJob;
import pe.edu.ta4.ruc.model.RucJobEvent;
import pe.edu.ta4.ruc.model.RucJobItem;
import pe.edu.ta4.ruc.model.StoredFile;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.repository.RucJobEventRepository;
import pe.edu.ta4.ruc.repository.RucJobItemRepository;
import pe.edu.ta4.ruc.repository.RucJobRepository;
import pe.edu.ta4.ruc.service.FileStorage;
import pe.edu.ta4.ruc.service.ReconciliationService;
import pe.edu.ta4.ruc.service.RucApiClient;
import pe.edu.ta4.ruc.service.ResultArchiveExtractor;
import pe.edu.ta4.ruc.service.RucExportService;
import pe.edu.ta4.ruc.service.RucJobService;
import pe.edu.ta4.ruc.service.RucTxtGenerator;
import pe.edu.ta4.ruc.service.RucWorkbookReader;
import pe.edu.ta4.ruc.service.SunatResultParser;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;

@Service
@Transactional
public class RucJobServiceImpl implements RucJobService {
    static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final int MAX_LIST = 100;
    /** Tope de consultas simultáneas a la API de RUC (el Excel trae 20, así que van todas a la vez; el límite de la API es 60 por minuto). */
    private static final int API_THREADS = 20;

    private final RucJobRepository jobs;
    private final RucJobItemRepository items;
    private final RucJobEventRepository events;
    private final RucWorkbookReader workbookReader;
    private final RucTxtGenerator txtGenerator;
    private final ResultArchiveExtractor archiveExtractor;
    private final SunatResultParser resultParser;
    private final ReconciliationService reconciliation;
    private final RucExportService exporter;
    private final FileStorage storage;
    private final Clock clock;
    private final RucApiClient apiClient;

    public RucJobServiceImpl(RucJobRepository jobs, RucJobItemRepository items, RucJobEventRepository events,
                             RucWorkbookReader workbookReader, RucTxtGenerator txtGenerator,
                             ResultArchiveExtractor archiveExtractor, SunatResultParser resultParser,
                             ReconciliationService reconciliation, RucExportService exporter,
                             FileStorage storage, Clock clock, RucApiClient apiClient) {
        this.apiClient = apiClient;
        this.jobs = jobs;
        this.items = items;
        this.events = events;
        this.workbookReader = workbookReader;
        this.txtGenerator = txtGenerator;
        this.archiveExtractor = archiveExtractor;
        this.resultParser = resultParser;
        this.reconciliation = reconciliation;
        this.exporter = exporter;
        this.storage = storage;
        this.clock = clock;
    }

    @Override
    public RucJobResponse create(String filename, byte[] workbook) {
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".xlsx") || workbook.length == 0) {
            throw ApiException.invalidWorkbook("Envía un archivo .xlsx no vacío en el campo file.",
                List.of(ProblemIssue.of("#/file", "NOT_A_WORKBOOK", "El archivo debe ser un Excel .xlsx no vacío.")));
        }
        List<RucEntry> entries = workbookReader.read(workbook);

        UUID id = UUID.randomUUID();
        // Con id asignado a mano, save() hace merge y devuelve OTRA instancia: solo esa está gestionada por JPA,
        // así que es la que hay que seguir modificando (si no, los cambios de estado no llegan a la base).
        RucJob job = jobs.save(new RucJob(id, entries.size(), displayName(filename), now()));
        items.saveAll(entries.stream().map(e -> new RucJobItem(UUID.randomUUID(), id, e.excelRow(), e.ruc())).toList());
        record(job, null, "Excel recibido: " + entries.size() + " RUC.");
        move(job, JobStatus.VALIDANDO, "Excel validado: formato, dígito verificador y duplicados.");

        try {
            storage.write(id, StoredFile.INPUT_EXCEL, workbook);
            storage.write(id, StoredFile.INPUT_TXT, txtGenerator.generate(entries.stream().map(RucEntry::ruc).toList()));
        } catch (RuntimeException e) {
            storage.deleteJob(id);
            throw e;
        }
        move(job, JobStatus.LISTO_PARA_ROBOT, "TXT generado para la Consulta Múltiple de SUNAT.");
        return RucJobResponse.from(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RucJobResponse> list(int limit) {
        int size = Math.clamp(limit, 1, MAX_LIST);
        return jobs.findAllByOrderByCreatedAtDesc(PageRequest.of(0, size)).stream().map(RucJobResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RucJobResponse get(UUID id) {
        return RucJobResponse.from(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RucJobEventResponse> events(UUID id) {
        find(id);
        return events.findByJobIdOrderBySequenceAsc(id).stream().map(RucJobEventResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload input(UUID id) {
        find(id);
        return new FileDownload("consulta_ruc_" + shortId(id) + ".txt", "text/plain; charset=utf-8",
            storage.read(id, StoredFile.INPUT_TXT));
    }

    @Override
    public RucJobResponse requestStart(UUID id) {
        RucJob job = jobs.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (job.getStatus() != JobStatus.LISTO_PARA_ROBOT && job.getStatus() != JobStatus.FALLIDO) {
            throw ApiException.conflict(job.getStatus().isFinal()
                ? "El trabajo ya terminó."
                : "El robot ya está en curso para este trabajo (estado " + job.getStatus() + ").");
        }
        if (job.getStartRequestedAt() != null) return RucJobResponse.from(job);
        job.requestStart(now());
        record(job, job.getStatus(), "Arranque del robot solicitado desde la web.");
        return RucJobResponse.from(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RucJobResponse> pendingStarts() {
        return jobs.findByStartRequestedAtIsNotNullOrderByStartRequestedAtAsc().stream().map(RucJobResponse::from).toList();
    }

    @Override
    public RucJobResponse claimStart(UUID id) {
        RucJob job = jobs.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (job.getStartRequestedAt() == null) {
            throw ApiException.conflict("No hay un arranque pendiente para este trabajo (ya lo tomó el lanzador o no se pidió).");
        }
        job.claimStart(now());
        record(job, job.getStatus(), "El lanzador tomó el trabajo; el robot está arrancando.");
        return RucJobResponse.from(job);
    }

    @Override
    public RucJobResponse reportStatus(UUID id, JobStatus status, String message) {
        if (status == null || !status.isRobotReportable()) {
            throw ApiException.badRequest("El robot solo puede reportar EN_CONSULTA, REQUIERE_INTERVENCION o FALLIDO.");
        }
        RucJob job = find(id);
        if (job.getStatus() == status) return RucJobResponse.from(job);
        if (!job.getStatus().canMoveTo(status)) {
            throw ApiException.conflict("El trabajo está en " + job.getStatus() + " y no puede pasar a " + status + ".");
        }
        move(job, status, message == null || message.isBlank() ? "Estado reportado por el robot." : message.trim());
        if (status == JobStatus.FALLIDO && apiClient.configured() && apiClient.autoFallback()) {
            // Respaldo automático: el robot falló (p. ej. SUNAT lo bloqueó), así que se intenta por API. Si la API
            // tampoco responde, el trabajo se queda en FALLIDO con el motivo en el historial.
            try {
                return fetchFromApi(job, true);
            } catch (ApiException unavailable) {
                return RucJobResponse.from(job);
            }
        }
        return RucJobResponse.from(job);
    }

    /**
     * {@code noRollbackFor}: un ZIP rechazado lanza 422 pero su registro en el historial debe conservarse. El
     * trabajo no se modifica antes de que el ZIP haya pasado todas las validaciones, así que no queda a medias.
     */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public RucJobResponse receiveResult(UUID id, byte[] zip) {
        RucJob job = jobs.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (job.getStatus().isFinal()) {
            throw ApiException.conflict("El resultado de este trabajo ya fue entregado.");
        }
        if (!job.getStatus().canReceiveResult()) {
            throw ApiException.conflict("El trabajo está en " + job.getStatus() + " y todavía no puede recibir resultados.");
        }

        byte[] txt;
        ParsedResult parsed;
        try {
            txt = archiveExtractor.extractTxt(zip);
            parsed = resultParser.parse(txt);
        } catch (ApiException rejected) {
            record(job, job.getStatus(), "Resultado rechazado: " + rejected.getMessage());
            throw rejected;
        }

        List<RucJobItem> rows = items.findByJobIdOrderByExcelRowAsc(id);
        Reconciliation outcome = reconciliation.reconcile(rows.stream().map(RucJobItem::getRuc).toList(), parsed);
        storage.write(id, StoredFile.RESULT_ZIP, zip);
        storage.write(id, StoredFile.RESULT_TXT, txt);

        move(job, JobStatus.PROCESANDO_RESULTADO, "ZIP de resultados recibido y validado.");
        return complete(job, rows, outcome, parsed, "SUNAT", "con datos de SUNAT");
    }

    /**
     * Fuente alternativa cuando SUNAT bloquea al robot: consulta cada RUC en una API de terceros y completa el
     * trabajo igual que con el ZIP. El trabajo queda marcado con su fuente (historial y resumen), para que nadie la
     * confunda con la Consulta Múltiple de SUNAT. Si la API falla a medias, el trabajo no se modifica.
     */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public RucJobResponse lookupViaApi(UUID id) {
        RucJob job = jobs.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (job.getStatus().isFinal()) {
            throw ApiException.conflict("El resultado de este trabajo ya fue entregado.");
        }
        if (!job.getStatus().canReceiveResult()) {
            throw ApiException.conflict("El trabajo está en " + job.getStatus() + " y todavía no puede recibir resultados.");
        }
        if (!apiClient.configured()) {
            throw ApiException.unavailable("La consulta por API no está configurada: falta el token (CONSULTA_RUC_API_TOKEN).");
        }
        return fetchFromApi(job, false);
    }

    /** Consulta cada RUC en la API y completa el trabajo; {@code automatic} indica que es el respaldo tras fallar el robot. */
    private RucJobResponse fetchFromApi(RucJob job, boolean automatic) {
        List<RucJobItem> rows = items.findByJobIdOrderByExcelRowAsc(job.getId());
        List<SunatRecord> records = new ArrayList<>();
        // En paralelo, un hilo por RUC (hasta el tope): 20 consultas una tras otra tardaban ~5 s; todas a la vez, ~1 s.
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(API_THREADS, rows.size())));
        try {
            List<CompletableFuture<Optional<SunatRecord>>> pending = rows.stream()
                .map(row -> CompletableFuture.supplyAsync(() -> apiClient.lookup(row.getRuc()), pool)).toList();
            for (CompletableFuture<Optional<SunatRecord>> lookup : pending) {
                try {
                    lookup.join().ifPresent(records::add);
                } catch (CompletionException failed) {
                    if (failed.getCause() instanceof ApiException api) throw api;
                    throw failed;
                }
            }
        } catch (ApiException unavailable) {
            record(job, job.getStatus(), "Consulta por API interrumpida: " + unavailable.getMessage());
            throw unavailable;
        } finally {
            pool.shutdownNow();
        }

        ParsedResult parsed = new ParsedResult(List.copyOf(records), List.of());
        Reconciliation outcome = reconciliation.reconcile(rows.stream().map(RucJobItem::getRuc).toList(), parsed);
        move(job, JobStatus.PROCESANDO_RESULTADO, (automatic
            ? "Respaldo automático: el robot falló y los datos se obtuvieron de la "
            : "Datos obtenidos de la ") + apiClient.sourceName() + " (no de la Consulta Múltiple de SUNAT).");
        return complete(job, rows, outcome, parsed, apiClient.sourceName(), "con datos (fuente: " + apiClient.sourceName() + ")");
    }

    /** Marca cada fila, aplica el resultado al trabajo y lo termina. Común al ZIP y a la API. */
    private RucJobResponse complete(RucJob job, List<RucJobItem> rows, Reconciliation outcome, ParsedResult parsed,
                                    String source, String summaryLabel) {
        for (RucJobItem row : rows) {
            var found = outcome.byRuc().get(row.getRuc());
            if (found != null) {
                row.markFound(found);
                if (outcome.duplicates().contains(row.getRuc())) {
                    row.addObservation(source + " devolvió este RUC más de una vez; se usó el primer registro.");
                }
            } else {
                row.markNotFound(source + " no devolvió datos para este RUC.");
            }
        }
        items.saveAll(rows);

        List<String> notes = summaryNotes(outcome, parsed);
        job.applyOutcome(outcome.byRuc().size(), outcome.missing().size(), outcome.unexpected().size(),
            summary(job, outcome, notes, summaryLabel));
        boolean clean = outcome.isClean() && parsed.warnings().isEmpty();
        move(job, clean ? JobStatus.COMPLETADO : JobStatus.COMPLETADO_CON_OBSERVACIONES,
            clean ? "Resultados procesados sin observaciones." : "Resultados procesados con observaciones.");
        return RucJobResponse.from(job);
    }

    @Override
    @Transactional(readOnly = true)
    public RucJobResultsResponse results(UUID id) {
        RucJob job = find(id);
        List<RucJobItemResponse> rows = items.findByJobIdOrderByExcelRowAsc(id).stream().map(RucJobItemResponse::from).toList();
        return new RucJobResultsResponse(id, job.getStatus(), rows);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload export(UUID id) {
        RucJob job = find(id);
        if (!job.getStatus().isFinal()) {
            throw ApiException.conflict("El trabajo aún no tiene resultados para exportar (estado " + job.getStatus() + ").");
        }
        byte[] excel = exporter.export(job, items.findByJobIdOrderByExcelRowAsc(id));
        return new FileDownload("ruc_resultado_" + shortId(id) + ".xlsx", XLSX, excel);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload resultFile(UUID id) {
        find(id);
        if (!storage.exists(id, StoredFile.RESULT_ZIP)) {
            throw ApiException.notFound("El trabajo todavía no tiene un archivo de resultados.");
        }
        return new FileDownload("resultado_sunat_" + shortId(id) + ".zip", "application/zip",
            storage.read(id, StoredFile.RESULT_ZIP));
    }

    private RucJob find(UUID id) {
        return jobs.findById(id).orElseThrow(() -> notFound(id));
    }

    private static ApiException notFound(UUID id) {
        return ApiException.notFound("No existe el trabajo " + id + ".");
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    /** Cambia de estado y lo anota en el historial. */
    private void move(RucJob job, JobStatus next, String message) {
        JobStatus from = job.getStatus();
        try {
            job.moveTo(next, now());
        } catch (IllegalStateException e) {
            throw ApiException.conflict("El trabajo está en " + from + " y no puede pasar a " + next + ".");
        }
        record(job, from, message);
    }

    private void record(RucJob job, JobStatus from, String message) {
        int sequence = events.countByJobId(job.getId()) + 1;
        String text = message.length() > 500 ? message.substring(0, 497) + "..." : message;
        events.save(new RucJobEvent(UUID.randomUUID(), job.getId(), sequence, now(), from, job.getStatus(), text));
    }

    private static List<String> summaryNotes(Reconciliation outcome, ParsedResult parsed) {
        List<String> notes = new ArrayList<>();
        if (!outcome.missing().isEmpty()) notes.add("sin datos: " + String.join(", ", outcome.missing()));
        if (!outcome.unexpected().isEmpty()) notes.add("no solicitados: " + String.join(", ", outcome.unexpected()));
        if (!outcome.duplicates().isEmpty()) notes.add("duplicados en SUNAT: " + String.join(", ", outcome.duplicates()));
        notes.addAll(parsed.warnings());
        return notes;
    }

    private static String summary(RucJob job, Reconciliation outcome, List<String> notes, String label) {
        String base = outcome.byRuc().size() + " de " + job.getTotal() + " RUC " + label;
        String text = notes.isEmpty() ? base + "." : base + "; " + String.join("; ", notes) + ".";
        return text.length() > 1000 ? text.substring(0, 997) + "..." : text;
    }

    /** Solo el nombre del archivo, sin rutas, y con largo acotado. */
    private static String displayName(String filename) {
        String name = filename.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}

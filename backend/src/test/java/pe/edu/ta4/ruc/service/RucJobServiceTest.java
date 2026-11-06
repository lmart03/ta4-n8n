package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pe.edu.ta4.ruc.RucProperties;
import pe.edu.ta4.ruc.dto.FileDownload;
import pe.edu.ta4.ruc.dto.RucJobEventResponse;
import pe.edu.ta4.ruc.dto.RucJobItemResponse;
import pe.edu.ta4.ruc.dto.RucJobResponse;
import pe.edu.ta4.ruc.model.ItemStatus;
import pe.edu.ta4.ruc.model.JobStatus;
import pe.edu.ta4.ruc.service.impl.LocalFileStorage;
import pe.edu.ta4.ruc.service.impl.ReconciliationServiceImpl;
import pe.edu.ta4.ruc.service.impl.ResultArchiveExtractorImpl;
import pe.edu.ta4.ruc.service.impl.RucExportServiceImpl;
import pe.edu.ta4.ruc.service.impl.RucJobServiceImpl;
import pe.edu.ta4.ruc.service.impl.RucTxtGeneratorImpl;
import pe.edu.ta4.ruc.service.impl.RucWorkbookReaderImpl;
import pe.edu.ta4.ruc.service.impl.SunatResultParserImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;
import pe.edu.ta4.shared.error.ProblemType;
import pe.edu.ta4.support.InMemoryRucRepositories;
import pe.edu.ta4.support.TestData;

/**
 * Flujo completo del servicio con los componentes reales y repositorios en memoria. Es la prueba de la Parte 1:
 * Excel de 20 RUC → TXT para SUNAT → ZIP real de SUNAT → resultados y Excel.
 */
class RucJobServiceTest {
    @TempDir
    Path storageDir;

    private InMemoryRucRepositories repos;
    private RucJobService service;
    private RucProperties properties;
    private final FakeApiClient apiClient = new FakeApiClient();

    /** API de RUC de mentira: devuelve un registro por RUC salvo los que se marquen como desconocidos. */
    private static final class FakeApiClient implements RucApiClient {
        boolean configured = true;
        boolean failing = false;
        boolean auto = false;
        final java.util.Set<String> unknown = new java.util.HashSet<>();

        @Override public boolean configured() { return configured; }
        @Override public boolean autoFallback() { return auto; }
        @Override public String sourceName() { return "API consultaderuc.pe"; }

        @Override
        public java.util.Optional<pe.edu.ta4.ruc.model.SunatRecord> lookup(String ruc) {
            if (failing) throw ApiException.unavailable("Se agotó la cuota de la API.");
            if (unknown.contains(ruc)) return java.util.Optional.empty();
            return java.util.Optional.of(new pe.edu.ta4.ruc.model.SunatRecord(ruc, "EMPRESA " + ruc, "AV. UNO, LIMA, LIMA, LIMA",
                "HABIDO", "ACTIVO", null));
        }
    }

    @BeforeEach
    void setUp() {
        repos = new InMemoryRucRepositories();
        properties = new RucProperties(storageDir, 20, 5_000_000, 20);
        service = new RucJobServiceImpl(repos.jobRepository, repos.itemRepository, repos.eventRepository,
            new RucWorkbookReaderImpl(properties), new RucTxtGeneratorImpl(), new ResultArchiveExtractorImpl(properties),
            new SunatResultParserImpl(), new ReconciliationServiceImpl(), new RucExportServiceImpl(),
            new LocalFileStorage(properties), Clock.fixed(Instant.parse("2026-11-06T15:00:00Z"), ZoneOffset.UTC),
            apiClient);
    }

    private RucJobResponse createJob() {
        return service.create("ruc_entrada.xlsx", TestData.workbookOf(TestData.twentyRucs()));
    }

    private static void assertProblem(Runnable action, ProblemType type) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class,
            error -> assertThat(error.type()).isEqualTo(type));
    }

    // ---------------------------------------------------------------- fuente alternativa (API)

    @Test
    void lookupViaApiCompletesTheJobAndSaysWhereTheDataCameFrom() {
        UUID id = createJob().id();

        RucJobResponse done = service.lookupViaApi(id);

        assertThat(done.status()).isEqualTo(JobStatus.COMPLETADO);
        assertThat(done.foundCount()).isEqualTo(20);
        assertThat(done.summary()).contains("fuente: API consultaderuc.pe");
        assertThat(service.events(id)).extracting(RucJobEventResponse::message)
            .anyMatch(message -> message.contains("no de la Consulta Múltiple de SUNAT"));
        assertThat(service.results(id).items()).allSatisfy(item -> {
            assertThat(item.status()).isEqualTo(ItemStatus.ENCONTRADO);
            assertThat(item.razonSocial()).startsWith("EMPRESA ");
        });
    }

    @Test
    void lookupViaApiMarksUnknownRucAsWithoutDataUsingTheRightSourceName() {
        UUID id = createJob().id();
        String missing = TestData.twentyRucs().getFirst();
        apiClient.unknown.add(missing);

        RucJobResponse done = service.lookupViaApi(id);

        assertThat(done.status()).isEqualTo(JobStatus.COMPLETADO_CON_OBSERVACIONES);
        assertThat(done.missingCount()).isEqualTo(1);
        assertThat(service.results(id).items().getFirst().observation()).isEqualTo("API consultaderuc.pe no devolvió datos para este RUC.");
    }

    @Test
    void lookupViaApiFailingHalfwayLeavesTheJobUntouchedAndExplainsWhy() {
        UUID id = createJob().id();
        apiClient.failing = true;

        assertProblem(() -> service.lookupViaApi(id), ProblemType.SERVICE_UNAVAILABLE);

        assertThat(service.get(id).status()).isEqualTo(JobStatus.LISTO_PARA_ROBOT);
        assertThat(service.events(id).getLast().message()).contains("Consulta por API interrumpida");
    }

    @Test
    void lookupViaApiWithoutTokenIsRejectedClearly() {
        UUID id = createJob().id();
        apiClient.configured = false;

        assertThatThrownBy(() -> service.lookupViaApi(id)).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.type()).isEqualTo(ProblemType.SERVICE_UNAVAILABLE);
            assertThat(error.getMessage()).contains("CONSULTA_RUC_API_TOKEN");
        });
    }

    @Test
    void whenTheRobotFailsTheApiTakesOverAutomaticallyAndSaysSo() {
        apiClient.auto = true;
        UUID id = createJob().id();

        RucJobResponse after = service.reportStatus(id, JobStatus.FALLIDO, "SUNAT rechazó el acceso");

        assertThat(after.status()).isEqualTo(JobStatus.COMPLETADO);
        assertThat(after.summary()).contains("fuente: API consultaderuc.pe");
        assertThat(service.events(id)).extracting(RucJobEventResponse::message)
            .anyMatch(message -> message.equals("SUNAT rechazó el acceso"))
            .anyMatch(message -> message.startsWith("Respaldo automático: el robot falló"));
    }

    @Test
    void ifTheApiAlsoFailsTheJobStaysFailedWithBothReasons() {
        apiClient.auto = true;
        apiClient.failing = true;
        UUID id = createJob().id();

        RucJobResponse after = service.reportStatus(id, JobStatus.FALLIDO, "SUNAT rechazó el acceso");

        assertThat(after.status()).isEqualTo(JobStatus.FALLIDO);
        assertThat(service.events(id)).extracting(RucJobEventResponse::message)
            .anyMatch(message -> message.equals("SUNAT rechazó el acceso"))
            .anyMatch(message -> message.contains("Consulta por API interrumpida"));
    }

    @Test
    void noAutomaticFallbackWhenItIsOffOrThereIsNoToken() {
        UUID id = createJob().id();
        assertThat(service.reportStatus(id, JobStatus.FALLIDO, "x").status()).isEqualTo(JobStatus.FALLIDO);

        apiClient.auto = true;
        apiClient.configured = false;
        UUID other = createJob().id();
        assertThat(service.reportStatus(other, JobStatus.FALLIDO, "x").status()).isEqualTo(JobStatus.FALLIDO);
    }

    @Test
    void lookupViaApiOnAFinishedJobIsAConflict() {
        UUID id = createJob().id();
        service.lookupViaApi(id);

        assertProblem(() -> service.lookupViaApi(id), ProblemType.CONFLICT);
    }

    // ---------------------------------------------------------------- crear

    @Test
    void createLeavesTheJobReadyForTheRobotWithTheTxtForSunat() throws Exception {
        RucJobResponse job = createJob();

        assertThat(job.status()).isEqualTo(JobStatus.LISTO_PARA_ROBOT);
        assertThat(job.total()).isEqualTo(20);
        assertThat(job.originalFilename()).isEqualTo("ruc_entrada.xlsx");
        assertThat(job.createdAt()).isEqualTo(Instant.parse("2026-11-06T15:00:00Z").atOffset(ZoneOffset.UTC));
        assertThat(repos.items).hasSize(20).allSatisfy(item -> assertThat(item.getStatus()).isEqualTo(ItemStatus.PENDIENTE));

        FileDownload input = service.input(job.id());
        assertThat(input.content()).isEqualTo(TestData.resource(TestData.SUNAT_INPUT));
        assertThat(input.contentType()).isEqualTo("text/plain; charset=utf-8");
        assertThat(Files.exists(storageDir.resolve(job.id().toString()).resolve("input.xlsx"))).isTrue();
    }

    @Test
    void createRecordsTheStateHistory() {
        RucJobResponse job = createJob();

        List<RucJobEventResponse> history = service.events(job.id());

        assertThat(history).extracting(RucJobEventResponse::sequence).containsExactly(1, 2, 3);
        assertThat(history).extracting(RucJobEventResponse::to)
            .containsExactly(JobStatus.RECIBIDO, JobStatus.VALIDANDO, JobStatus.LISTO_PARA_ROBOT);
        assertThat(history.getFirst().from()).isNull();
        assertThat(history.get(1).from()).isEqualTo(JobStatus.RECIBIDO);
    }

    @Test
    void createRejectsAnInvalidExcelWithoutCreatingAnything() throws Exception {
        byte[] bad = TestData.workbook("RUC", "ruc", "20100047218", "20100047219", "20100047218");

        assertThatThrownBy(() -> service.create("ruc.xlsx", bad)).isInstanceOfSatisfying(ApiException.class, error -> {
            assertProblemType(error, ProblemType.INVALID_WORKBOOK);
            assertThat(error.issues()).extracting(ProblemIssue::code)
                .containsExactly("RUC_INVALID_CHECK_DIGIT", "RUC_DUPLICATE");
        });
        assertThat(repos.jobs).isEmpty();
        assertThat(repos.items).isEmpty();
        try (var files = Files.list(storageDir)) {
            assertThat(files.count()).isZero();
        }
    }

    @Test
    void createRejectsAFileThatIsNotXlsxOrIsEmpty() {
        assertProblem(() -> service.create("ruc.csv", TestData.workbookOf(TestData.twentyRucs())), ProblemType.INVALID_WORKBOOK);
        assertProblem(() -> service.create("ruc.xlsx", new byte[0]), ProblemType.INVALID_WORKBOOK);
        assertProblem(() -> service.create(null, new byte[] {1}), ProblemType.INVALID_WORKBOOK);
    }

    @Test
    void createKeepsOnlyTheFileNameNotThePath() {
        RucJobResponse job = service.create("C:\\Users\\yo\\..\\docs\\ruc.xlsx", TestData.workbookOf(TestData.twentyRucs()));

        assertThat(job.originalFilename()).isEqualTo("ruc.xlsx");
    }

    private static void assertProblemType(ApiException error, ProblemType type) {
        assertThat(error.type()).isEqualTo(type);
    }

    // -------------------------------------------------------------- consultas

    @Test
    void getAndListAndNotFound() {
        RucJobResponse first = createJob();
        RucJobResponse second = createJob();

        assertThat(service.get(first.id()).id()).isEqualTo(first.id());
        assertThat(service.list(10)).hasSize(2);
        assertThat(service.list(1)).hasSize(1);
        assertThat(service.list(0)).hasSize(1);
        assertProblem(() -> service.get(UUID.randomUUID()), ProblemType.NOT_FOUND);
        assertProblem(() -> service.events(UUID.randomUUID()), ProblemType.NOT_FOUND);
        assertProblem(() -> service.input(UUID.randomUUID()), ProblemType.NOT_FOUND);
        assertThat(second.id()).isNotEqualTo(first.id());
    }

    // ------------------------------------------------------------ estado robot

    @Test
    void robotReportsProgressAndHumanIntervention() {
        UUID id = createJob().id();

        assertThat(service.reportStatus(id, JobStatus.EN_CONSULTA, "Subiendo el TXT").status()).isEqualTo(JobStatus.EN_CONSULTA);
        assertThat(service.reportStatus(id, JobStatus.REQUIERE_INTERVENCION, "Hay una verificación").status())
            .isEqualTo(JobStatus.REQUIERE_INTERVENCION);
        assertThat(service.reportStatus(id, JobStatus.EN_CONSULTA, null).status()).isEqualTo(JobStatus.EN_CONSULTA);

        assertThat(service.events(id)).extracting(RucJobEventResponse::message)
            .contains("Subiendo el TXT", "Hay una verificación", "Estado reportado por el robot.");
    }

    @Test
    void reportingTheSameStateTwiceDoesNothing() {
        UUID id = createJob().id();
        service.reportStatus(id, JobStatus.EN_CONSULTA, null);
        int events = service.events(id).size();

        service.reportStatus(id, JobStatus.EN_CONSULTA, null);

        assertThat(service.events(id)).hasSize(events);
    }

    @Test
    void robotCannotReportStatesThatBelongToTheBackend() {
        UUID id = createJob().id();

        assertProblem(() -> service.reportStatus(id, JobStatus.COMPLETADO, null), ProblemType.VALIDATION_ERROR);
        assertProblem(() -> service.reportStatus(id, JobStatus.PROCESANDO_RESULTADO, null), ProblemType.VALIDATION_ERROR);
        assertProblem(() -> service.reportStatus(id, null, null), ProblemType.VALIDATION_ERROR);
    }

    @Test
    void aFailedJobCanBeRetriedAndAFinishedOneCannotBeTouched() {
        UUID id = createJob().id();
        service.reportStatus(id, JobStatus.FALLIDO, "Se cayó el navegador");
        assertThat(service.reportStatus(id, JobStatus.EN_CONSULTA, "Reintento").status()).isEqualTo(JobStatus.EN_CONSULTA);

        service.receiveResult(id, TestData.sunatZip());

        assertProblem(() -> service.reportStatus(id, JobStatus.EN_CONSULTA, null), ProblemType.CONFLICT);
    }

    // ------------------------------------------------------- arranque del robot

    @Test
    void startRequestPutsTheJobInTheQueueAndLeavesATrace() {
        UUID id = createJob().id();

        RucJobResponse asked = service.requestStart(id);

        assertThat(asked.startRequestedAt()).isNotNull();
        assertThat(asked.status()).isEqualTo(JobStatus.LISTO_PARA_ROBOT);
        assertThat(service.pendingStarts()).extracting(RucJobResponse::id).containsExactly(id);
        assertThat(service.events(id).getLast().message()).isEqualTo("Arranque del robot solicitado desde la web.");
    }

    @Test
    void askingTwiceDoesNotDuplicateAnything() {
        UUID id = createJob().id();
        service.requestStart(id);
        int events = service.events(id).size();

        service.requestStart(id);

        assertThat(service.events(id)).hasSize(events);
        assertThat(service.pendingStarts()).hasSize(1);
    }

    @Test
    void claimTakesTheJobOutOfTheQueue() {
        UUID id = createJob().id();
        service.requestStart(id);

        RucJobResponse claimed = service.claimStart(id);

        assertThat(claimed.startRequestedAt()).isNull();
        assertThat(service.pendingStarts()).isEmpty();
        assertThat(service.events(id).getLast().message()).startsWith("El lanzador tomó el trabajo");
    }

    @Test
    void onlyOneLauncherCanClaimTheSameRequest() {
        UUID id = createJob().id();
        service.requestStart(id);
        service.claimStart(id);

        assertProblem(() -> service.claimStart(id), ProblemType.CONFLICT);
    }

    @Test
    void claimWithoutARequestIs409() {
        UUID id = createJob().id();

        assertProblem(() -> service.claimStart(id), ProblemType.CONFLICT);
    }

    @Test
    void queueIsOrderedFromTheOldestRequest() {
        UUID first = createJob().id();
        UUID second = createJob().id();
        service.requestStart(second);
        service.requestStart(first);

        assertThat(service.pendingStarts()).hasSize(2);
    }

    @Test
    void startCannotBeRequestedWhileTheRobotRunsOrAfterTheEnd() {
        UUID running = createJob().id();
        service.reportStatus(running, JobStatus.EN_CONSULTA, null);
        UUID finished = createJob().id();
        service.receiveResult(finished, TestData.sunatZip());

        assertProblem(() -> service.requestStart(running), ProblemType.CONFLICT);
        assertProblem(() -> service.requestStart(finished), ProblemType.CONFLICT);
        assertProblem(() -> service.requestStart(UUID.randomUUID()), ProblemType.NOT_FOUND);
    }

    @Test
    void aFailedJobCanBeStartedAgain() {
        UUID id = createJob().id();
        service.reportStatus(id, JobStatus.FALLIDO, "Se cayó el navegador");

        RucJobResponse retry = service.requestStart(id);

        assertThat(retry.status()).isEqualTo(JobStatus.FALLIDO);
        assertThat(retry.startRequestedAt()).isNotNull();
        service.claimStart(id);
        assertThat(service.reportStatus(id, JobStatus.EN_CONSULTA, "Reintento").status()).isEqualTo(JobStatus.EN_CONSULTA);
    }

    @Test
    void whenTheRobotStartsRunningTheRequestIsGone() {
        UUID id = createJob().id();
        service.requestStart(id);

        service.reportStatus(id, JobStatus.EN_CONSULTA, null);

        assertThat(service.get(id).startRequestedAt()).isNull();
        assertThat(service.pendingStarts()).isEmpty();
    }

    // ---------------------------------------------------------------- resultado

    @Test
    void realSunatZipCompletesTheJobWithoutObservations() {
        UUID id = createJob().id();

        RucJobResponse done = service.receiveResult(id, TestData.sunatZip());

        assertThat(done.status()).isEqualTo(JobStatus.COMPLETADO);
        assertThat(done.foundCount()).isEqualTo(20);
        assertThat(done.missingCount()).isZero();
        assertThat(done.unexpectedCount()).isZero();
        assertThat(done.completedAt()).isNotNull();
        assertThat(done.summary()).isEqualTo("20 de 20 RUC con datos de SUNAT.");
    }

    @Test
    void resultsAreNormalizedInTheOrderOfTheExcel() {
        UUID id = createJob().id();
        service.receiveResult(id, TestData.sunatZip());

        List<RucJobItemResponse> rows = service.results(id).items();

        assertThat(rows).hasSize(20);
        assertThat(rows.stream().map(RucJobItemResponse::ruc)).containsExactlyElementsOf(TestData.twentyRucs());
        RucJobItemResponse bcp = rows.getFirst();
        assertThat(bcp.status()).isEqualTo(ItemStatus.ENCONTRADO);
        assertThat(bcp.razonSocial()).isEqualTo("BANCO DE CREDITO DEL PERU");
        assertThat(bcp.direccionFiscal()).isEqualTo("JR. CENTENARIO NRO. 156 URB. LADERAS DE MELGAREJO, LA MOLINA, LIMA, LIMA");
        assertThat(bcp.condicionContribuyente()).isEqualTo("HABIDO");
        assertThat(bcp.estadoContribuyente()).isEqualTo("ACTIVO");
        assertThat(bcp.fechaInicioActividades()).hasToString("1889-04-09");
        assertThat(rows).allSatisfy(row -> assertThat(row.condicionContribuyente()).isEqualTo("HABIDO"));
    }

    @Test
    void missingRucsFinishWithObservations() {
        UUID id = createJob().id();
        byte[] zip = TestData.zip("RM1.txt", TestData.sunatResultWithout("20100047218", "20100028698"));

        RucJobResponse done = service.receiveResult(id, zip);

        assertThat(done.status()).isEqualTo(JobStatus.COMPLETADO_CON_OBSERVACIONES);
        assertThat(done.foundCount()).isEqualTo(18);
        assertThat(done.missingCount()).isEqualTo(2);
        assertThat(done.summary()).contains("18 de 20").contains("sin datos: 20100047218, 20100028698");

        List<RucJobItemResponse> rows = service.results(id).items();
        assertThat(rows.getFirst().status()).isEqualTo(ItemStatus.NO_ENCONTRADO);
        assertThat(rows.getFirst().observation()).contains("SUNAT no devolvió datos");
        assertThat(rows.getFirst().razonSocial()).isNull();
        assertThat(rows.get(1).status()).isEqualTo(ItemStatus.ENCONTRADO);
    }

    @Test
    void unexpectedRucsAreCountedButNotMixedIn() {
        UUID id = service.create("ruc.xlsx", TestData.workbook("RUC", "ruc", "20100047218", "20100053455")).id();

        RucJobResponse done = service.receiveResult(id, TestData.sunatZip());

        assertThat(done.status()).isEqualTo(JobStatus.COMPLETADO_CON_OBSERVACIONES);
        assertThat(done.foundCount()).isEqualTo(2);
        assertThat(done.unexpectedCount()).isEqualTo(18);
        assertThat(service.results(id).items()).hasSize(2);
    }

    @Test
    void theOriginalFilesAreKeptUntouched() throws Exception {
        UUID id = createJob().id();
        byte[] zip = TestData.sunatZip();
        service.receiveResult(id, zip);

        Path folder = storageDir.resolve(id.toString());
        assertThat(Files.readAllBytes(folder.resolve("result.zip"))).isEqualTo(zip);
        assertThat(Files.readAllBytes(folder.resolve("result.txt"))).isEqualTo(TestData.resource(TestData.SUNAT_RESULT));
        assertThat(service.resultFile(id).content()).isEqualTo(zip);
        assertThat(service.resultFile(id).contentType()).isEqualTo("application/zip");
    }

    @Test
    void theResultCanOnlyBeDeliveredOnce() {
        UUID id = createJob().id();
        service.receiveResult(id, TestData.sunatZip());

        assertProblem(() -> service.receiveResult(id, TestData.sunatZip()), ProblemType.CONFLICT);
        assertThat(service.get(id).status()).isEqualTo(JobStatus.COMPLETADO);
    }

    @Test
    void aRejectedZipLeavesTheJobUntouchedAndLeavesATraceInTheHistory() {
        UUID id = createJob().id();
        int eventsBefore = service.events(id).size();

        assertThatThrownBy(() -> service.receiveResult(id, "no soy un zip".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertProblemType(error, ProblemType.INVALID_RESULT_FILE);
                assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("NOT_A_ZIP");
            });

        assertThat(service.get(id).status()).isEqualTo(JobStatus.LISTO_PARA_ROBOT);
        assertThat(service.results(id).items()).allSatisfy(item -> assertThat(item.status()).isEqualTo(ItemStatus.PENDIENTE));
        List<RucJobEventResponse> history = service.events(id);
        assertThat(history).hasSize(eventsBefore + 1);
        assertThat(history.getLast().message()).startsWith("Resultado rechazado");
        assertThat(history.getLast().to()).isEqualTo(JobStatus.LISTO_PARA_ROBOT);
        assertProblem(() -> service.resultFile(id), ProblemType.NOT_FOUND);
    }

    @Test
    void aCorrectZipIsAcceptedAfterARejectedOne() {
        UUID id = createJob().id();
        assertProblem(() -> service.receiveResult(id, new byte[] {1, 2, 3}), ProblemType.INVALID_RESULT_FILE);

        assertThat(service.receiveResult(id, TestData.sunatZip()).status()).isEqualTo(JobStatus.COMPLETADO);
    }

    @Test
    void aZipWithUnsafeEntriesIsRejected() {
        UUID id = createJob().id();

        assertProblem(() -> service.receiveResult(id, TestData.zip("../evil.txt", "x")), ProblemType.INVALID_RESULT_FILE);
        assertThat(service.get(id).status()).isEqualTo(JobStatus.LISTO_PARA_ROBOT);
    }

    @Test
    void resultOfAnUnknownJobIsNotFound() {
        assertProblem(() -> service.receiveResult(UUID.randomUUID(), TestData.sunatZip()), ProblemType.NOT_FOUND);
    }

    // ------------------------------------------------------------------- Excel

    @Test
    void exportOnlyWorksOnceTheJobIsComplete() {
        UUID id = createJob().id();

        assertProblem(() -> service.export(id), ProblemType.CONFLICT);

        service.receiveResult(id, TestData.sunatZip());
        FileDownload excel = service.export(id);

        assertThat(excel.filename()).isEqualTo("ruc_resultado_" + id.toString().substring(0, 8) + ".xlsx");
        assertThat(excel.contentType()).contains("spreadsheetml");
        assertThat(excel.content()).startsWith((byte) 'P', (byte) 'K');
    }
}

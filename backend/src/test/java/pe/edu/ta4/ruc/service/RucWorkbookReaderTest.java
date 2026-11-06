package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.RucProperties;
import pe.edu.ta4.ruc.model.RucEntry;
import pe.edu.ta4.ruc.service.impl.RucWorkbookReaderImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;
import pe.edu.ta4.shared.error.ProblemType;
import pe.edu.ta4.support.TestData;

class RucWorkbookReaderTest {
    private final RucWorkbookReader reader =
        new RucWorkbookReaderImpl(new RucProperties(Path.of("unused"), 20, 5_000_000, 20));

    @Test
    void readsTheTwentyRealRucsInOrder() {
        List<RucEntry> entries = reader.read(TestData.workbookOf(TestData.twentyRucs()));

        assertThat(entries).hasSize(20);
        assertThat(entries.stream().map(RucEntry::ruc)).containsExactlyElementsOf(TestData.twentyRucs());
        assertThat(entries.getFirst().excelRow()).isEqualTo(2);
        assertThat(entries.getLast().excelRow()).isEqualTo(21);
    }

    @Test
    void acceptsNumericCells() {
        List<RucEntry> entries = reader.read(TestData.workbook("RUC", "ruc", 20100047218.0, "20100053455"));

        assertThat(entries.stream().map(RucEntry::ruc)).containsExactly("20100047218", "20100053455");
    }

    @Test
    void usesTheFirstSheetAndAnyHeaderCaseWhenThereIsNoRucSheet() {
        List<RucEntry> entries = reader.read(TestData.workbook("Hoja1", "RUC", "20100047218"));

        assertThat(entries).extracting(RucEntry::ruc).containsExactly("20100047218");
    }

    @Test
    void ignoresBlankRows() {
        List<RucEntry> entries = reader.read(TestData.workbook("RUC", "ruc", "20100047218", null, "20100053455"));

        assertThat(entries).extracting(RucEntry::ruc).containsExactly("20100047218", "20100053455");
        assertThat(entries.get(1).excelRow()).isEqualTo(4);
    }

    @Test
    void reportsEveryProblemAtOnce() {
        byte[] workbook = TestData.workbook("RUC", "ruc",
            "20100047218",   // fila 2: válido
            "12345",         // fila 3: formato
            "20100047219",   // fila 4: dígito verificador
            "30100047218",   // fila 5: prefijo
            "20100047218",   // fila 6: duplicado
            "");             // fila 7: vacío con datos en otra columna

        assertThatThrownBy(() -> reader.read(workbook)).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.type()).isEqualTo(ProblemType.INVALID_WORKBOOK);
            assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly(
                "RUC_INVALID_FORMAT", "RUC_INVALID_CHECK_DIGIT", "RUC_INVALID_PREFIX", "RUC_DUPLICATE", "RUC_EMPTY");
            assertThat(error.issues()).extracting(ProblemIssue::pointer).containsExactly(
                "#/rows/3/ruc", "#/rows/4/ruc", "#/rows/5/ruc", "#/rows/6/ruc", "#/rows/7/ruc");
        });
    }

    @Test
    void rejectsMoreThanTheMaximum() {
        Object[] rows = java.util.stream.IntStream.range(0, 21).mapToObj(TestData::validRuc).toArray();

        assertThatThrownBy(() -> reader.read(TestData.workbook("RUC", "ruc", rows)))
            .isInstanceOfSatisfying(ApiException.class, error ->
                assertThat(error.issues()).extracting(ProblemIssue::code).contains("TOO_MANY_RUC"));
    }

    @Test
    void aStrayNoteBelowTheTableIsReportedAsThatRowNotAsTooMany() {
        Object[] rows = new Object[22];
        for (int i = 0; i < 20; i++) rows[i] = TestData.validRuc(i);
        rows[20] = null;
        rows[21] = "Total de empresas";   // fila 23 del Excel

        assertThatThrownBy(() -> reader.read(TestData.workbook("RUC", "ruc", rows)))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("RUC_INVALID_FORMAT");
                assertThat(error.issues().getFirst().pointer()).isEqualTo("#/rows/23/ruc");
                assertThat(error.issues().getFirst().detail()).contains("Fila 23").contains("«Total de empresas»");
            });
    }

    @Test
    void anOverlongCellIsQuotedTruncated() {
        String huge = "x".repeat(200);

        assertThatThrownBy(() -> reader.read(TestData.workbook("RUC", "ruc", huge)))
            .isInstanceOfSatisfying(ApiException.class, error ->
                assertThat(error.issues().getFirst().detail()).contains("«" + "x".repeat(24) + "…»"));
    }

    @Test
    void acceptsExactlyTheMaximum() {
        Object[] rows = java.util.stream.IntStream.range(0, 20).mapToObj(TestData::validRuc).toArray();

        assertThat(reader.read(TestData.workbook("RUC", "ruc", rows))).hasSize(20);
    }

    @Test
    void rejectsAWorkbookWithoutRucs() {
        assertThatThrownBy(() -> reader.read(TestData.workbook("RUC", "ruc")))
            .isInstanceOfSatisfying(ApiException.class, error ->
                assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("NO_RUC"));
    }

    @Test
    void rejectsAWorkbookWithoutTheRucColumn() {
        assertThatThrownBy(() -> reader.read(TestData.workbook("RUC", "documento", "20100047218")))
            .isInstanceOfSatisfying(ApiException.class, error ->
                assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("MISSING_RUC_COLUMN"));
    }

    @Test
    void rejectsBytesThatAreNotAWorkbook() {
        assertThatThrownBy(() -> reader.read("esto no es un excel".getBytes(StandardCharsets.UTF_8)))
            .isInstanceOfSatisfying(ApiException.class, error -> {
                assertThat(error.type()).isEqualTo(ProblemType.INVALID_WORKBOOK);
                assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly("NOT_A_WORKBOOK");
            });
    }
}

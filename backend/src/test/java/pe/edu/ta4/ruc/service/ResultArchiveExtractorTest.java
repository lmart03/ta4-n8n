package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.RucProperties;
import pe.edu.ta4.ruc.service.impl.ResultArchiveExtractorImpl;
import pe.edu.ta4.shared.error.ApiException;
import pe.edu.ta4.shared.error.ProblemIssue;
import pe.edu.ta4.shared.error.ProblemType;
import pe.edu.ta4.support.TestData;

class ResultArchiveExtractorTest {
    private final ResultArchiveExtractor extractor =
        new ResultArchiveExtractorImpl(new RucProperties(Path.of("unused"), 20, 5_000, 5));

    private static void assertRejected(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.type()).isEqualTo(ProblemType.INVALID_RESULT_FILE);
            assertThat(error.issues()).extracting(ProblemIssue::code).containsExactly(code);
        });
    }

    @Test
    void extractsTheTxtFromARealSunatZip() {
        ResultArchiveExtractor generous =
            new ResultArchiveExtractorImpl(new RucProperties(Path.of("unused"), 20, 5_000_000, 20));

        assertThat(generous.extractTxt(TestData.sunatZip())).isEqualTo(TestData.resource(TestData.SUNAT_RESULT));
    }

    @Test
    void ignoresOtherEntriesButStillReadsTheTxt() {
        byte[] zip = TestData.zip("informe.docx", "contenido ajeno", "RM1.txt", "linea");

        assertThat(extractor.extractTxt(zip)).isEqualTo("linea".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsPathTraversalNames() {
        assertRejected(() -> extractor.extractTxt(TestData.zip("../evil.txt", "x")), "ZIP_UNSAFE_ENTRY");
        assertRejected(() -> extractor.extractTxt(TestData.zip("a/../../evil.txt", "x")), "ZIP_UNSAFE_ENTRY");
        assertRejected(() -> extractor.extractTxt(TestData.zip("/etc/passwd.txt", "x")), "ZIP_UNSAFE_ENTRY");
        assertRejected(() -> extractor.extractTxt(TestData.zip("C:/windows/x.txt", "x")), "ZIP_UNSAFE_ENTRY");
    }

    @Test
    void rejectsAZipWithoutTxt() {
        assertRejected(() -> extractor.extractTxt(TestData.zip("a.docx", "x")), "NO_TXT_IN_ZIP");
    }

    @Test
    void rejectsAZipWithSeveralTxt() {
        assertRejected(() -> extractor.extractTxt(TestData.zip("a.txt", "x", "b.txt", "y")), "MULTIPLE_TXT_IN_ZIP");
    }

    @Test
    void rejectsBytesThatAreNotAZip() {
        assertRejected(() -> extractor.extractTxt("no soy un zip".getBytes(StandardCharsets.UTF_8)), "NOT_A_ZIP");
        assertRejected(() -> extractor.extractTxt(new byte[0]), "NOT_A_ZIP");
    }

    @Test
    void stopsAZipBombByLimitingUncompressedSize() {
        byte[] zeros = new byte[2_000_000];
        byte[] zip = TestData.zip("bomba.txt", zeros);

        assertThat(zip.length).isLessThan(5_000);
        assertRejected(() -> extractor.extractTxt(zip), "ZIP_TOO_LARGE");
    }

    @Test
    void limitIncludesEntriesThatWouldBeIgnored() {
        byte[] zip = TestData.zip("relleno.bin", new byte[2_000_000], "RM1.txt", "ok");

        assertRejected(() -> extractor.extractTxt(zip), "ZIP_TOO_LARGE");
    }

    @Test
    void rejectsTooManyEntries() {
        Object[] entries = new Object[14];
        for (int i = 0; i < 7; i++) {
            entries[i * 2] = "f" + i + ".bin";
            entries[i * 2 + 1] = "x";
        }

        assertRejected(() -> extractor.extractTxt(TestData.zip(entries)), "ZIP_TOO_MANY_ENTRIES");
    }
}

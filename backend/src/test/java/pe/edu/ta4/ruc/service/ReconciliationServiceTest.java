package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.model.ParsedResult;
import pe.edu.ta4.ruc.model.Reconciliation;
import pe.edu.ta4.ruc.model.SunatRecord;
import pe.edu.ta4.ruc.service.impl.ReconciliationServiceImpl;

class ReconciliationServiceTest {
    private final ReconciliationService service = new ReconciliationServiceImpl();

    private static SunatRecord record(String ruc, String name) {
        return new SunatRecord(ruc, name, "DIR", "HABIDO", "ACTIVO", LocalDate.of(2000, 1, 1));
    }

    @Test
    void cleanWhenEverythingMatches() {
        Reconciliation result = service.reconcile(List.of("A", "B"),
            new ParsedResult(List.of(record("A", "uno"), record("B", "dos")), List.of()));

        assertThat(result.isClean()).isTrue();
        assertThat(result.byRuc()).containsOnlyKeys("A", "B");
    }

    @Test
    void reportsMissingRucs() {
        Reconciliation result = service.reconcile(List.of("A", "B", "C"),
            new ParsedResult(List.of(record("B", "dos")), List.of()));

        assertThat(result.missing()).containsExactly("A", "C");
        assertThat(result.isClean()).isFalse();
    }

    @Test
    void reportsUnexpectedRucsOnce() {
        Reconciliation result = service.reconcile(List.of("A"),
            new ParsedResult(List.of(record("A", "uno"), record("X", "ajeno"), record("X", "ajeno")), List.of()));

        assertThat(result.unexpected()).containsExactly("X");
        assertThat(result.byRuc()).containsOnlyKeys("A");
        assertThat(result.isClean()).isFalse();
    }

    @Test
    void keepsTheFirstRecordOfADuplicatedRuc() {
        Reconciliation result = service.reconcile(List.of("A"),
            new ParsedResult(List.of(record("A", "primero"), record("A", "segundo")), List.of()));

        assertThat(result.duplicates()).containsExactly("A");
        assertThat(result.byRuc().get("A").razonSocial()).isEqualTo("primero");
        assertThat(result.isClean()).isFalse();
    }
}

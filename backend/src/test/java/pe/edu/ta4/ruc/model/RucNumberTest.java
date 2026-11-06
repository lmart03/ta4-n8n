package pe.edu.ta4.ruc.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import pe.edu.ta4.support.TestData;

class RucNumberTest {

    @Test
    void acceptsTheTwentyRealRucsFromSunat() {
        assertThat(TestData.twentyRucs()).hasSize(20).allSatisfy(ruc -> {
            assertThat(RucNumber.hasValidFormat(ruc)).isTrue();
            assertThat(RucNumber.hasValidPrefix(ruc)).isTrue();
            assertThat(RucNumber.hasValidCheckDigit(ruc)).isTrue();
        });
    }

    @Test
    void rejectsWrongCheckDigit() {
        assertThat(RucNumber.hasValidCheckDigit("20100047218")).isTrue();
        assertThat(RucNumber.hasValidCheckDigit("20100047219")).isFalse();
    }

    @Test
    void rejectsBadFormat() {
        assertThat(RucNumber.hasValidFormat(null)).isFalse();
        assertThat(RucNumber.hasValidFormat("")).isFalse();
        assertThat(RucNumber.hasValidFormat("2010004721")).isFalse();
        assertThat(RucNumber.hasValidFormat("201000472181")).isFalse();
        assertThat(RucNumber.hasValidFormat("2010004721A")).isFalse();
        assertThat(RucNumber.hasValidCheckDigit("2010004721A")).isFalse();
    }

    @Test
    void onlyKnownPrefixesAreAccepted() {
        assertThat(RucNumber.hasValidPrefix("10723842234")).isTrue();
        assertThat(RucNumber.hasValidPrefix("20100047218")).isTrue();
        assertThat(RucNumber.hasValidPrefix("30100047218")).isFalse();
        assertThat(RucNumber.hasValidPrefix("99999999999")).isFalse();
    }

    @Test
    void syntheticRucsHelperProducesValidNumbers() {
        for (int n = 0; n < 50; n++) {
            assertThat(RucNumber.hasValidCheckDigit(TestData.validRuc(n))).isTrue();
        }
    }
}

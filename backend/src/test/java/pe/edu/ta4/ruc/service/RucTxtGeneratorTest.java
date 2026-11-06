package pe.edu.ta4.ruc.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.edu.ta4.ruc.service.impl.RucTxtGeneratorImpl;
import pe.edu.ta4.support.TestData;

class RucTxtGeneratorTest {
    private final RucTxtGenerator generator = new RucTxtGeneratorImpl();

    @Test
    void producesExactlyTheFileThatWasAcceptedBySunat() {
        byte[] generated = generator.generate(TestData.twentyRucs());

        assertThat(generated).isEqualTo(TestData.resource(TestData.SUNAT_INPUT));
    }

    @Test
    void writesOneRucPerLineEndingInPipeWithCrlf() {
        String text = new String(generator.generate(List.of("20100047218", "20100053455")), StandardCharsets.UTF_8);

        assertThat(text).isEqualTo("20100047218|\r\n20100053455|\r\n");
    }

    @Test
    void hasNoByteOrderMark() {
        byte[] bytes = generator.generate(List.of("20100047218"));

        assertThat(bytes[0]).isEqualTo((byte) '2');
    }
}

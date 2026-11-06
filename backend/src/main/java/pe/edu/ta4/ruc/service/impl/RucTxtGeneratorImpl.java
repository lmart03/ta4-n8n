package pe.edu.ta4.ruc.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.edu.ta4.ruc.service.RucTxtGenerator;

@Service
public class RucTxtGeneratorImpl implements RucTxtGenerator {
    private static final String LINE_END = "\r\n";

    @Override
    public byte[] generate(List<String> rucs) {
        StringBuilder text = new StringBuilder();
        for (String ruc : rucs) {
            text.append(ruc).append('|').append(LINE_END);
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }
}

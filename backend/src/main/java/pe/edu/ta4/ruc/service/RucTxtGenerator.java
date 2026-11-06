package pe.edu.ta4.ruc.service;

import java.util.List;

public interface RucTxtGenerator {
    /**
     * Genera el TXT para la Consulta Múltiple de SUNAT: un RUC por línea, terminado en {@code |},
     * UTF-8 sin BOM y saltos CRLF.
     */
    byte[] generate(List<String> rucs);
}

package pe.edu.ta4.ruc.model;

import java.util.List;

/** Resultado de leer el TXT de SUNAT: los registros válidos y los avisos de filas problemáticas. */
public record ParsedResult(List<SunatRecord> records, List<String> warnings) {
}

package pe.edu.ta4.ruc.service;

import pe.edu.ta4.ruc.model.ParsedResult;

public interface SunatResultParser {
    /**
     * Lee el TXT de resultados de la Consulta Múltiple de SUNAT (campos separados por {@code |}).
     * Identifica las columnas por su nombre, no por posición; tolera el {@code |} final, los espacios de
     * relleno y los {@code -} que SUNAT usa para "sin dato". Lanza un 422 si faltan columnas obligatorias.
     */
    ParsedResult parse(byte[] txt);
}

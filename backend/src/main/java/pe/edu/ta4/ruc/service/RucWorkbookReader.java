package pe.edu.ta4.ruc.service;

import java.util.List;
import pe.edu.ta4.ruc.model.RucEntry;

public interface RucWorkbookReader {
    /**
     * Lee los RUC de un Excel .xlsx (hoja {@code RUC} o, si no existe, la primera; columna {@code ruc}).
     * Valida formato, prefijo, dígito verificador, duplicados y cantidad. Si algo falla, lanza un 422
     * con todos los problemas encontrados a la vez, no solo el primero.
     */
    List<RucEntry> read(byte[] workbook);
}

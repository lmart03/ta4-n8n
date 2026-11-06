package pe.edu.ta4.ruc.service;

import java.util.List;
import pe.edu.ta4.ruc.model.RucJob;
import pe.edu.ta4.ruc.model.RucJobItem;

public interface RucExportService {
    /** Genera el Excel de resultados (hoja Resultados con los campos pedidos y hoja Resumen). */
    byte[] export(RucJob job, List<RucJobItem> items);
}
